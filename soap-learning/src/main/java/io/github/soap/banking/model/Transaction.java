package io.github.soap.banking.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record Transaction(
        String transactionId,
        String accountNumber,
        String transactionReference,
        String transactionType,
        BigDecimal amount,
        BigDecimal availableBalance,
        String currency,
        String description,
        LocalDateTime timestamp
) {
}