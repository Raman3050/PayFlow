package com.payflow.transaction.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {
    private Long transactionId;
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String currency;
    private Instant timestamp;

    // Explicit getters for Kafka/Jackson in case Data/Builder interaction fails
    public Long getTransactionId() { return transactionId; }
    public Long getFromWalletId() { return fromWalletId; }
    public Long getToWalletId() { return toWalletId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getTimestamp() { return timestamp; }
}