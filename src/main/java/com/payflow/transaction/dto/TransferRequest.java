package com.payflow.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotNull(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "From wallet ID is required")
    private Long fromWalletId;

    @NotNull(message = "To wallet ID is required")
    private Long toWalletId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "USD";
}