package com.payflow.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long id;
    private Long userId;
    private String currency;
    private String status;
    /** Derived from the ledger: SUM(credits) - SUM(debits). */
    private BigDecimal balance;
    private Long version;
    private Instant createdAt;
}
