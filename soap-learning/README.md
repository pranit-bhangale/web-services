# Logix Banking SOAP Service

A thread-safe Spring Boot SOAP web service for banking operations with a unified XSD schema (Logix).

## Features

- **Unified XSD Schema** (`logix.xsd`): Single schema defining all banking operations
- **Thread-Safe Implementation**: ConcurrentHashMap + Atomic operations for high concurrency
- **Idempotent Transactions**: Duplicate transaction references are safely handled
- **Comprehensive Operations**:
  - Create Account
  - Credit/Deposit Amount
  - Debit/Withdraw Amount
  - Fetch Balance
  - Get Account Details
  - Get Transaction History (with pagination & filtering)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SOAP Endpoint (AccountEndpoint)          │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                  Account Service (AccountServiceImpl)       │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│              Account Repository (AccountRepository)         │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │  ConcurrentHashMap│  │  ConcurrentHashMap│               │
│  │   Accounts       │  │   Transactions   │               │
│  └─────────────────┘  └─────────────────┘                   │
│  ┌─────────────────────────────────────────┐               │
│  │  Account Transaction Index (per account) │              │
│  └─────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

## Thread Safety

The implementation uses multiple layers of thread safety:

1. **Account Storage**: `ConcurrentHashMap<String, Account>` for O(1) atomic reads/writes
2. **Transaction Storage**: `ConcurrentHashMap<String, Transaction>` with `putIfAbsent` for idempotency
3. **Atomic Balance Updates**: `ConcurrentHashMap.compute()` for atomic read-modify-write
4. **Transaction Index**: Per-account `ConcurrentHashMap` with synchronized lists
5. **Sequence Generation**: `AtomicLong` for account/transaction IDs

### Credit/Debit Processing Flow

```java
// 1. Validate inputs (amount > 0, reference not blank)
if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalArgumentException("Amount must be greater than zero");
}

// 2. Check idempotency (synchronized on transaction reference)
Transaction existing = transactions.get(transactionReference);
if (existing != null) {
    return existing; // Return existing for idempotency
}

// 3. Atomic account update with balance validation
Account updatedAccount = accounts.compute(accountNumber, (_, existingAccount) -> {
    if (existingAccount == null) throw new IllegalArgumentException("Account not found");
    if (!"ACTIVE".equals(existingAccount.accountStatus())) {
        throw new IllegalArgumentException("Account not active");
    }
    
    if (isCredit) {
        return existingAccount.withBalance(existingAccount.balance().add(amount));
    } else {
        // Debit: check sufficient funds
        if (existingAccount.balance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        return existingAccount.withBalance(existingAccount.balance().subtract(amount));
    }
});

// 4. Create and store transaction
Transaction transaction = Transaction.builder()
    .transactionId("TXN" + transactionSequence.incrementAndGet())
    .accountNumber(accountNumber)
    .transactionReference(transactionReference)
    .transactionType(transactionType)
    .amount(amount)
    .availableBalance(updatedAccount.balance())
    .currency(currency)
    .description(description)
    .timestamp(LocalDateTime.now())
    .build();

transactions.putIfAbsent(transactionReference, transaction);
```

## XSD Schema (logix.xsd)

The unified schema defines:

### Simple Types
- `AccountType`: SAVINGS, CURRENT, FIXED_DEPOSIT, RECURRING_DEPOSIT
- `TransactionType`: CREDIT, DEBIT, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
- `CurrencyCode`: ISO 4217 3-letter codes
- `PositiveDecimal`: > 0
- `NonNegativeDecimal`: >= 0
- `AccountStatus`: ACTIVE, INACTIVE, SUSPENDED, CLOSED, DORMANT

### Operations

| Operation | Request | Response |
|-----------|---------|----------|
| Create Account | CreateAccountRequest | CreateAccountResponse |
| Credit Amount | CreditAccountRequest | CreditAccountResponse |
| Debit Amount | DebitAccountRequest | DebitAccountResponse |
| Fetch Balance | FetchBalanceRequest | FetchBalanceResponse |
| Get Account Details | GetAccountDetailsRequest | GetAccountDetailsResponse |
| Transaction History | GetTransactionHistoryRequest | GetTransactionHistoryResponse |

## Building & Running

```bash
# Build
./mvnw clean compile

# Run tests
./mvnw test

# Run application
./mvnw spring-boot:run
```

The SOAP endpoint will be available at: `http://localhost:8080/ws`

WSDL: `http://localhost:8080/ws/banking.wsdl`

## Testing SOAP Operations

### Create Account
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:bnk="http://www.github.com/io/github/soap/banking">
    <soap:Body>
        <bnk:CreateAccountRequest>
            <bnk:customerName>John Doe</bnk:customerName>
            <bnk:email>john@example.com</bnk:email>
            <bnk:mobileNumber>9876543210</bnk:mobileNumber>
            <bnk:accountType>SAVINGS</bnk:accountType>
            <bnk:initialBalance>1000.00</bnk:initialBalance>
            <bnk:currency>INR</bnk:currency>
        </bnk:CreateAccountRequest>
    </soap:Body>
</soap:Envelope>
```

### Credit Amount
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:bnk="http://www.github.com/io/github/soap/banking">
    <soap:Body>
        <bnk:CreditAccountRequest>
            <bnk:accountNumber>ACC100001</bnk:accountNumber>
            <bnk:amount>500.00</bnk:amount>
            <bnk:transactionReference>TXN-001</bnk:transactionReference>
            <bnk:description>Salary deposit</bnk:description>
        </bnk:CreditAccountRequest>
    </soap:Body>
</soap:Envelope>
```

### Debit Amount
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:bnk="http://www.github.com/io/github/soap/banking">
    <soap:Body>
        <bnk:DebitAccountRequest>
            <bnk:accountNumber>ACC100001</bnk:accountNumber>
            <bnk:amount>200.00</bnk:amount>
            <bnk:transactionReference>TXN-002</bnk:transactionReference>
            <bnk:description>ATM withdrawal</bnk:description>
        </bnk:DebitAccountRequest>
    </soap:Body>
</soap:Envelope>
```

### Fetch Balance
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:bnk="http://www.github.com/io/github/soap/banking">
    <soap:Body>
        <bnk:FetchBalanceRequest>
            <bnk:accountNumber>ACC100001</bnk:accountNumber>
        </bnk:FetchBalanceRequest>
    </soap:Body>
</soap:Envelope>
```

### Get Transaction History
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:bnk="http://www.github.com/io/github/soap/banking">
    <soap:Body>
        <bnk:GetTransactionHistoryRequest>
            <bnk:accountNumber>ACC100001</bnk:accountNumber>
            <bnk:fromDate>2024-01-01T00:00:00</bnk:fromDate>
            <bnk:toDate>2024-12-31T23:59:59</bnk:toDate>
            <bnk:transactionType>CREDIT</bnk:transactionType>
            <bnk:pageNumber>0</bnk:pageNumber>
            <bnk:pageSize>20</bnk:pageSize>
        </bnk:GetTransactionHistoryRequest>
    </soap:Body>
</soap:Envelope>
```

## Concurrency Test Results

The implementation passes rigorous concurrency tests:

- **100 threads × 10 credits each**: Final balance = 10,000.00 (expected)
- **50 threads × 20 mixed operations**: Final balance = 35,000.00 (expected)
- **Idempotency**: Duplicate transaction references return same transaction
- **Insufficient funds**: Properly rejected with balance unchanged

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── io/github/soap/
│   │       ├── banking/
│   │       │   ├── endpoint/AccountEndpoint.java
│   │       │   ├── model/Account.java, Transaction.java
│   │       │   ├── repository/AccountRepository.java
│   │       │   └── service/AccountService.java, AccountServiceImpl.java
│   │       └── config/SoapConfig.java
│   └── resources/
│       └── xsd/logix.xsd          # Unified XSD schema
└── test/
    └── java/io/github/soap/SoapApplicationTests.java
```

## Technologies

- Java 25
- Spring Boot 4.1.2
- Spring Web Services
- JAXB (jaxb2-maven-plugin)
- Lombok
- JUnit 5