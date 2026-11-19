package io.github.soap.banking.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record Account(
        String accountNumber,
        String customerName,
        String email,
        String mobileNumber,
        String accountType,
        BigDecimal balance,
        String currency,
        String accountStatus,
        LocalDateTime createdDate,
        LocalDateTime lastTransactionDate
) {

    public Account withBalance(BigDecimal newBalance) {
        return Account.builder()
                .accountNumber(accountNumber)
                .customerName(customerName)
                .email(email)
                .mobileNumber(mobileNumber)
                .accountType(accountType)
                .balance(newBalance)
                .currency(currency)
                .accountStatus(accountStatus)
                .createdDate(createdDate)
                .lastTransactionDate(LocalDateTime.now())
                .build();
    }

    public Account withStatus(String newStatus) {
        return Account.builder()
                .accountNumber(accountNumber)
                .customerName(customerName)
                .email(email)
                .mobileNumber(mobileNumber)
                .accountType(accountType)
                .balance(balance)
                .currency(currency)
                .accountStatus(newStatus)
                .createdDate(createdDate)
                .lastTransactionDate(lastTransactionDate)
                .build();
    }

    public Account withLastTransactionDate(LocalDateTime lastTransactionDate) {
        return Account.builder()
                .accountNumber(accountNumber)
                .customerName(customerName)
                .email(email)
                .mobileNumber(mobileNumber)
                .accountType(accountType)
                .balance(balance)
                .currency(currency)
                .accountStatus(accountStatus)
                .createdDate(createdDate)
                .lastTransactionDate(lastTransactionDate)
                .build();
    }
}