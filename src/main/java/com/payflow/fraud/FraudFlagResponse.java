package com.payflow.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** What the admin fraud review queue needs for each flag. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudFlagResponse {
    private Long id;
    private String rule;
    private String description;
    private boolean reviewed;
    private Instant createdAt;

    private Long transactionId;
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String currency;

    private String fromWalletStatus;
    private String fromUserEmail;
}
