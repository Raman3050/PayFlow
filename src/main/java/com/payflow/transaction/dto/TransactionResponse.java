package com.payflow.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String idempotencyKey;
    private Long fromWallet;
    private Long toWallet;
    private java.math.BigDecimal amount;
    private String currency;
    private String status;
    private String failureReason;
    private Instant createdAt;
}