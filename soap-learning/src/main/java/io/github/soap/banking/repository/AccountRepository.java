package io.github.soap.banking.repository;

import io.github.soap.banking.model.Account;
import io.github.soap.banking.model.Transaction;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class AccountRepository {

    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> accountTransactionIndex = new ConcurrentHashMap<>();

    private final AtomicLong accountSequence = new AtomicLong(100000);
    private final AtomicLong transactionSequence = new AtomicLong(1000);

    private static @NonNull BigDecimal getNewBalance(BigDecimal amount, boolean isCredit, Account existingAccount) {
        BigDecimal currentBalance = existingAccount.balance();
        BigDecimal newBalance;

        if (isCredit) {
            newBalance = currentBalance.add(amount);
        } else {
            // Debit: check sufficient funds
            if (currentBalance.compareTo(amount) < 0) {
                throw new IllegalArgumentException(
                        String.format("Insufficient balance. Available: %s, Required: %s",
                                currentBalance, amount));
            }
            newBalance = currentBalance.subtract(amount);
        }
        return newBalance;
    }

    public Account createAccount(
            String customerName, String email, String mobileNumber,
            String accountType, BigDecimal initialBalance, String currency
    ) {
        String accountNumber = "ACC" + accountSequence.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customerName(customerName)
                .email(email)
                .mobileNumber(mobileNumber)
                .accountType(accountType)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .currency(currency != null ? currency : "INR")
                .accountStatus("ACTIVE")
                .createdDate(now)
                .lastTransactionDate(now)
                .build();

        accounts.put(accountNumber, account);
        accountTransactionIndex.put(accountNumber, Collections.synchronizedList(new ArrayList<>()));
        return account;
    }

    public Transaction credit(String accountNumber, BigDecimal amount, String transactionReference,
                              String description, String currency) {
        return processTransaction(accountNumber, amount, transactionReference, description,
                currency, "CREDIT", true);
    }

    public Transaction debit(String accountNumber, BigDecimal amount, String transactionReference,
                             String description, String currency) {
        return processTransaction(accountNumber, amount, transactionReference, description,
                currency, "DEBIT", false);
    }

    private Transaction processTransaction(
            String accountNumber, BigDecimal amount,
            String transactionReference, String description,
            String currency, String transactionType,
            boolean isCredit
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException("Transaction reference is required");
        }
        if (currency == null || currency.isBlank()) currency = "INR";
        Transaction existingTransaction = transactions.get(transactionReference);
        if (existingTransaction != null) {
            if (!existingTransaction.accountNumber().equals(accountNumber)
                    || existingTransaction.amount().compareTo(amount) != 0
                    || !existingTransaction.transactionType().equals(transactionType)) {
                throw new IllegalArgumentException("Transaction reference already used for a different transaction");
            }
            return existingTransaction;
        }

        Account updatedAccount = accounts.compute(accountNumber, (_, existingAccount) -> {
            if (existingAccount == null) {
                throw new IllegalArgumentException("Account not found: " + accountNumber);
            }
            if (!"ACTIVE".equals(existingAccount.accountStatus())) {
                throw new IllegalArgumentException("Account is not active: " + existingAccount.accountStatus());
            }
            BigDecimal newBalance = getNewBalance(amount, isCredit, existingAccount);
            return existingAccount.withBalance(newBalance);
        });
        String transactionId = "TXN" + transactionSequence.incrementAndGet();
        LocalDateTime timestamp = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .accountNumber(accountNumber)
                .transactionReference(transactionReference)
                .transactionType(transactionType)
                .amount(amount)
                .availableBalance(updatedAccount.balance())
                .currency(currency)
                .description(description)
                .timestamp(timestamp)
                .build();

        Transaction previous = transactions.putIfAbsent(transactionReference, transaction);
        if (previous != null) return previous;
        accountTransactionIndex.computeIfAbsent(accountNumber, _ ->
                Collections.synchronizedList(new ArrayList<>())).add(transactionReference);
        accounts.computeIfPresent(accountNumber, (_, acc) ->
                acc.withLastTransactionDate(timestamp));
        return transaction;
    }

    public Account findByAccountNumber(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return account;
    }

    public List<Transaction> getTransactionHistory(
            String accountNumber, LocalDateTime fromDate,
            LocalDateTime toDate, String transactionType,
            int pageNumber, int pageSize
    ) {
        List<String> transactionRefs = accountTransactionIndex.get(accountNumber);
        if (transactionRefs == null) return Collections.emptyList();
        List<String> refsCopy = new ArrayList<>(transactionRefs);
        return refsCopy.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .filter(tx -> fromDate == null || !tx.timestamp().isBefore(fromDate))
                .filter(tx -> toDate == null || !tx.timestamp().isAfter(toDate))
                .filter(tx -> transactionType == null || transactionType.equals(tx.transactionType()))
                .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    public int getTransactionCount(
            String accountNumber, LocalDateTime fromDate,
            LocalDateTime toDate, String transactionType
    ) {
        List<String> transactionRefs = accountTransactionIndex.get(accountNumber);
        if (transactionRefs == null) return 0;
        return (int) transactionRefs.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .filter(tx -> fromDate == null || !tx.timestamp().isBefore(fromDate))
                .filter(tx -> toDate == null || !tx.timestamp().isAfter(toDate))
                .filter(tx -> transactionType == null || transactionType.equals(tx.transactionType()))
                .count();
    }

    public Account updateAccountStatus(String accountNumber, String newStatus) {
        return accounts.compute(accountNumber, (_, existing) -> {
            if (existing == null) {
                throw new IllegalArgumentException("Account not found: " + accountNumber);
            }
            return existing.withStatus(newStatus);
        });
    }

    public boolean exists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    public List<Account> getAllAccounts() {
        return accounts.values().stream()
                .filter(acc -> "ACTIVE".equals(acc.accountStatus()))
                .collect(Collectors.toList());
    }
}