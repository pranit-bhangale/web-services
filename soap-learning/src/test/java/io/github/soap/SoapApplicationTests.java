package io.github.soap;

import io.github.soap.banking.model.Account;
import io.github.soap.banking.repository.AccountRepository;
import io.github.soap.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SoapApplicationTests {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testCreateAccount() {
        Account account = accountRepository.createAccount(
                "John Doe",
                "john@example.com",
                "9876543210",
                "SAVINGS",
                new BigDecimal("1000.00"),
                "INR"
        );

        assertNotNull(account);
        assertNotNull(account.accountNumber());
        assertEquals("John Doe", account.customerName());
        assertEquals("john@example.com", account.email());
        assertEquals("9876543210", account.mobileNumber());
        assertEquals("SAVINGS", account.accountType());
        assertEquals(new BigDecimal("1000.00"), account.balance());
        assertEquals("INR", account.currency());
        assertEquals("ACTIVE", account.accountStatus());
    }

    @Test
    void testCreditAndDebit() {
        Account account = accountRepository.createAccount(
                "Jane Smith",
                "jane@example.com",
                "9876543211",
                "CURRENT",
                new BigDecimal("5000.00"),
                "INR"
        );

        // Credit
        var creditTx = accountRepository.credit(
                account.accountNumber(),
                new BigDecimal("1000.00"),
                "TXN-CREDIT-001",
                "Salary deposit",
                "INR"
        );

        assertNotNull(creditTx);
        assertEquals("CREDIT", creditTx.transactionType());
        assertEquals(new BigDecimal("1000.00"), creditTx.amount());
        assertEquals(new BigDecimal("6000.00"), creditTx.availableBalance());

        // Debit
        var debitTx = accountRepository.debit(
                account.accountNumber(),
                new BigDecimal("500.00"),
                "TXN-DEBIT-001",
                "ATM withdrawal",
                "INR"
        );

        assertNotNull(debitTx);
        assertEquals("DEBIT", debitTx.transactionType());
        assertEquals(new BigDecimal("500.00"), debitTx.amount());
        assertEquals(new BigDecimal("5500.00"), debitTx.availableBalance());

        // Verify final balance
        Account updatedAccount = accountRepository.findByAccountNumber(account.accountNumber());
        assertEquals(new BigDecimal("5500.00"), updatedAccount.balance());
    }

    @Test
    void testInsufficientFunds() {
        Account account = accountRepository.createAccount(
                "Bob Wilson",
                "bob@example.com",
                "9876543212",
                "SAVINGS",
                new BigDecimal("100.00"),
                "INR"
        );

        assertThrows(IllegalArgumentException.class, () ->
                accountRepository.debit(
                        account.accountNumber(),
                        new BigDecimal("200.00"),
                        "TXN-DEBIT-002",
                        "Overdraft attempt",
                        "INR"
                )
        );

        // Balance should remain unchanged
        Account updatedAccount = accountRepository.findByAccountNumber(account.accountNumber());
        assertEquals(new BigDecimal("100.00"), updatedAccount.balance());
    }

    @Test
    void testIdempotentTransaction() {
        Account account = accountRepository.createAccount(
                "Alice Brown",
                "alice@example.com",
                "9876543213",
                "SAVINGS",
                new BigDecimal("1000.00"),
                "INR"
        );

        // First credit with reference
        var tx1 = accountRepository.credit(
                account.accountNumber(),
                new BigDecimal("500.00"),
                "TXN-IDEMPOTENT-001",
                "First credit",
                "INR"
        );

        // Second credit with same reference (should return same transaction)
        var tx2 = accountRepository.credit(
                account.accountNumber(),
                new BigDecimal("500.00"),
                "TXN-IDEMPOTENT-001",
                "Duplicate credit",
                "INR"
        );

        assertEquals(tx1.transactionId(), tx2.transactionId());
        assertEquals(tx1.transactionReference(), tx2.transactionReference());

        // Balance should only be credited once
        Account updatedAccount = accountRepository.findByAccountNumber(account.accountNumber());
        assertEquals(new BigDecimal("1500.00"), updatedAccount.balance());
    }

    @Test
    void testConcurrentCredits() throws InterruptedException {
        Account account = accountRepository.createAccount(
                "Concurrent User",
                "concurrent@example.com",
                "9876543214",
                "SAVINGS",
                new BigDecimal("0.00"),
                "INR"
        );

        int threadCount = 100;
        int creditsPerThread = 10;
        BigDecimal amountPerCredit = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < creditsPerThread; j++) {
                        String ref = String.format("CONCURRENT-%d-%d", threadNum, j);
                        accountRepository.credit(
                                account.accountNumber(),
                                amountPerCredit,
                                ref,
                                "Concurrent credit",
                                "INR"
                        );
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount, successCount.get());
        assertEquals(0, failureCount.get());

        // Verify final balance: 100 threads * 10 credits * 10.00 = 10000.00
        BigDecimal expectedBalance = new BigDecimal(String.valueOf(threadCount * creditsPerThread * 10));
        Account updatedAccount = accountRepository.findByAccountNumber(account.accountNumber());
        assertEquals(0, expectedBalance.compareTo(updatedAccount.balance()));
    }

    @Test
    void testConcurrentCreditsAndDebits() throws InterruptedException {
        Account account = accountRepository.createAccount(
                "Mixed User",
                "mixed@example.com",
                "9876543215",
                "CURRENT",
                new BigDecimal("10000.00"),
                "INR"
        );

        int threadCount = 50;
        int operationsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String ref = String.format("MIXED-%d-%d", threadNum, j);
                        if (j % 2 == 0) {
                            accountRepository.credit(
                                    account.accountNumber(),
                                    new BigDecimal("100.00"),
                                    ref + "-C",
                                    "Credit",
                                    "INR"
                            );
                        } else {
                            accountRepository.debit(
                                    account.accountNumber(),
                                    new BigDecimal("50.00"),
                                    ref + "-D",
                                    "Debit",
                                    "INR"
                            );
                        }
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount, successCount.get());
        // Balance should be: 10000 + (50 threads * 10 credits * 100) - (50 threads * 10 debits * 50)
        // = 10000 + 50000 - 25000 = 35000
        BigDecimal expectedBalance = new BigDecimal("35000.00");
        Account updatedAccount = accountRepository.findByAccountNumber(account.accountNumber());
        assertEquals(expectedBalance, updatedAccount.balance());
    }

    @Test
    void testTransactionHistory() throws InterruptedException {
        Account account = accountRepository.createAccount(
                "History User",
                "history@example.com",
                "9876543216",
                "SAVINGS",
                new BigDecimal("1000.00"),
                "INR"
        );

        accountRepository.credit(account.accountNumber(), new BigDecimal("500.00"),
                "TXN-HIST-001", "Deposit 1", "INR");
        Thread.sleep(10); // Ensure different timestamps
        accountRepository.debit(account.accountNumber(), new BigDecimal("200.00"),
                "TXN-HIST-002", "Withdrawal 1", "INR");
        Thread.sleep(10);
        accountRepository.credit(account.accountNumber(), new BigDecimal("300.00"),
                "TXN-HIST-003", "Deposit 2", "INR");

        var history = accountRepository.getTransactionHistory(
                account.accountNumber(), null, null, null, 0, 10);

        assertEquals(3, history.size());
        // Should be in reverse chronological order (newest first)
        assertEquals("TXN-HIST-003", history.get(0).transactionReference());
        assertEquals("TXN-HIST-002", history.get(1).transactionReference());
        assertEquals("TXN-HIST-001", history.get(2).transactionReference());

        int totalCount = accountRepository.getTransactionCount(
                account.accountNumber(), null, null, null);
        assertEquals(3, totalCount);
    }

    @Test
    void testAccountStatusUpdate() {
        Account account = accountRepository.createAccount(
                "Status User",
                "status@example.com",
                "9876543217",
                "SAVINGS",
                new BigDecimal("1000.00"),
                "INR"
        );

        Account suspended = accountRepository.updateAccountStatus(account.accountNumber(), "SUSPENDED");
        assertEquals("SUSPENDED", suspended.accountStatus());

        Account reactivated = accountRepository.updateAccountStatus(account.accountNumber(), "ACTIVE");
        assertEquals("ACTIVE", reactivated.accountStatus());

        // Operations should fail on suspended account
        accountRepository.updateAccountStatus(account.accountNumber(), "SUSPENDED");
        assertThrows(IllegalArgumentException.class, () ->
                accountRepository.credit(
                        account.accountNumber(),
                        new BigDecimal("100.00"),
                        "TXN-STATUS-001",
                        "Credit on suspended",
                        "INR"
                )
        );
    }
}