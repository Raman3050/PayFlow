package com.payflow.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Idempotency key is required")
    @Size(max = 128, message = "Idempotency key must be at most 128 characters")
    private String idempotencyKey;

    /**
     * Optional. The sender is always the authenticated user's wallet; if this is sent,
     * it must match that wallet.
     */
    private Long fromWalletId;

    @NotNull(message = "Recipient wallet ID is required")
    private Long toWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Amount can have at most 2 decimal places")
    private BigDecimal amount;

    private String currency = "USD";
}
