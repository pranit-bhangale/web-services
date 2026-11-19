package io.github.soap.banking.service;

import io.github.soap.banking.model.Account;
import io.github.soap.banking.model.Transaction;
import io.github.soap.banking.repository.AccountRepository;
import io.github.soap.dto.AccountStatus;
import io.github.soap.dto.AccountType;
import io.github.soap.dto.CreateAccountRequest;
import io.github.soap.dto.CreateAccountResponse;
import io.github.soap.dto.CreditAccountRequest;
import io.github.soap.dto.CreditAccountResponse;
import io.github.soap.dto.DebitAccountRequest;
import io.github.soap.dto.DebitAccountResponse;
import io.github.soap.dto.FetchBalanceRequest;
import io.github.soap.dto.FetchBalanceResponse;
import io.github.soap.dto.GetAccountDetailsRequest;
import io.github.soap.dto.GetAccountDetailsResponse;
import io.github.soap.dto.GetTransactionHistoryRequest;
import io.github.soap.dto.GetTransactionHistoryResponse;
import io.github.soap.dto.TransactionRecord;
import io.github.soap.dto.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.GregorianCalendar;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DatatypeFactory", e);
        }
    }

    private final AccountRepository accountRepository;

    private XMLGregorianCalendar toXmlGregorianCalendar(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(localDateTime.atZone(java.time.ZoneId.systemDefault()));
        return DATATYPE_FACTORY.newXMLGregorianCalendar(gregorianCalendar);
    }

    private AccountType toDtoAccountType(String accountType) {
        return AccountType.fromValue(accountType);
    }

    private AccountStatus toDtoAccountStatus(String accountStatus) {
        return AccountStatus.fromValue(accountStatus);
    }

    private TransactionType toDtoTransactionType(String transactionType) {
        return TransactionType.fromValue(transactionType);
    }

    @Override
    public CreateAccountResponse createNewAccount(CreateAccountRequest request) {
        if (request.getInitialBalance() == null
                || request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        String currency = request.getCurrency() != null ? request.getCurrency() : "INR";
        Account account = accountRepository.createAccount(
                request.getCustomerName(),
                request.getEmail(),
                request.getMobileNumber(),
                request.getAccountType().value(),
                request.getInitialBalance(),
                currency
        );

        CreateAccountResponse response = new CreateAccountResponse();
        response.setAccountNumber(account.accountNumber());
        response.setCustomerName(account.customerName());
        response.setEmail(account.email());
        response.setMobileNumber(account.mobileNumber());
        response.setAccountType(toDtoAccountType(account.accountType()));
        response.setBalance(account.balance());
        response.setCurrency(account.currency());
        response.setAccountStatus(toDtoAccountStatus(account.accountStatus()));
        response.setStatus("SUCCESS");
        response.setMessage("Account created successfully");
        return response;
    }

    @Override
    public CreditAccountResponse creditAmount(CreditAccountRequest request) {
        Transaction transaction = accountRepository.credit(
                request.getAccountNumber(),
                request.getAmount(),
                request.getTransactionReference(),
                request.getDescription(),
                "INR"
        );

        CreditAccountResponse response = new CreditAccountResponse();
        response.setAccountNumber(transaction.accountNumber());
        response.setTransactionId(transaction.transactionId());
        response.setTransactionReference(transaction.transactionReference());
        response.setTransactionType(toDtoTransactionType(transaction.transactionType()));
        response.setCreditedAmount(transaction.amount());
        response.setAvailableBalance(transaction.availableBalance());
        response.setCurrency(transaction.currency());
        response.setTransactionTimestamp(toXmlGregorianCalendar(transaction.timestamp()));
        response.setStatus("SUCCESS");
        response.setMessage("Amount credited successfully");
        return response;
    }

    @Override
    public DebitAccountResponse debitAmount(DebitAccountRequest request) {
        Transaction transaction = accountRepository.debit(
                request.getAccountNumber(),
                request.getAmount(),
                request.getTransactionReference(),
                request.getDescription(),
                "INR"
        );

        DebitAccountResponse response = new DebitAccountResponse();
        response.setAccountNumber(transaction.accountNumber());
        response.setTransactionId(transaction.transactionId());
        response.setTransactionReference(transaction.transactionReference());
        response.setTransactionType(toDtoTransactionType(transaction.transactionType()));
        response.setDebitedAmount(transaction.amount());
        response.setAvailableBalance(transaction.availableBalance());
        response.setCurrency(transaction.currency());
        response.setTransactionTimestamp(toXmlGregorianCalendar(transaction.timestamp()));
        response.setStatus("SUCCESS");
        response.setMessage("Amount debited successfully");
        return response;
    }

    @Override
    public FetchBalanceResponse fetchBalance(FetchBalanceRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

        FetchBalanceResponse response = new FetchBalanceResponse();
        response.setAccountNumber(account.accountNumber());
        response.setCustomerName(account.customerName());
        response.setAccountType(toDtoAccountType(account.accountType()));
        response.setAvailableBalance(account.balance());
        response.setCurrency(account.currency());
        response.setAccountStatus(toDtoAccountStatus(account.accountStatus()));
        response.setLastTransactionTimestamp(toXmlGregorianCalendar(account.lastTransactionDate()));
        response.setStatus("SUCCESS");
        response.setMessage("Balance fetched successfully");
        return response;
    }

    @Override
    public GetAccountDetailsResponse getAccountDetails(GetAccountDetailsRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

        GetAccountDetailsResponse response = new GetAccountDetailsResponse();
        response.setAccountNumber(account.accountNumber());
        response.setCustomerName(account.customerName());
        response.setEmail(account.email());
        response.setMobileNumber(account.mobileNumber());
        response.setAccountType(toDtoAccountType(account.accountType()));
        response.setBalance(account.balance());
        response.setCurrency(account.currency());
        response.setAccountStatus(toDtoAccountStatus(account.accountStatus()));
        response.setCreatedDate(toXmlGregorianCalendar(account.createdDate()));
        response.setLastTransactionDate(toXmlGregorianCalendar(account.lastTransactionDate()));
        response.setStatus("SUCCESS");
        response.setMessage("Account details fetched successfully");
        return response;
    }

    @Override
    public GetTransactionHistoryResponse getTransactionHistory(GetTransactionHistoryRequest request) {
        String accountNumber = request.getAccountNumber();
        accountRepository.findByAccountNumber(accountNumber);
        LocalDateTime fromDate = request.getFromDate() != null
                ? request.getFromDate().toGregorianCalendar().toZonedDateTime().toLocalDateTime()
                : null;
        LocalDateTime toDate = request.getToDate() != null
                ? request.getToDate().toGregorianCalendar().toZonedDateTime().toLocalDateTime()
                : null;
        String transactionType = request.getTransactionType() != null
                ? request.getTransactionType().value()
                : null;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        List<Transaction> transactions = accountRepository.getTransactionHistory(
                accountNumber, fromDate, toDate, transactionType, pageNumber, pageSize);
        int totalCount = accountRepository.getTransactionCount(
                accountNumber, fromDate, toDate, transactionType);

        GetTransactionHistoryResponse response = new GetTransactionHistoryResponse();
        response.setAccountNumber(accountNumber);
        response.setTotalCount(totalCount);
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setStatus("SUCCESS");
        response.setMessage("Transaction history fetched successfully");

        for (Transaction tx : transactions) {
            TransactionRecord record = new TransactionRecord();
            record.setTransactionId(tx.transactionId());
            record.setTransactionReference(tx.transactionReference());
            record.setTransactionType(toDtoTransactionType(tx.transactionType()));
            record.setAmount(tx.amount());
            record.setAvailableBalance(tx.availableBalance());
            record.setCurrency(tx.currency());
            record.setDescription(tx.description());
            record.setTimestamp(toXmlGregorianCalendar(tx.timestamp()));
            response.getTransactions().add(record);
        }

        return response;
    }
}