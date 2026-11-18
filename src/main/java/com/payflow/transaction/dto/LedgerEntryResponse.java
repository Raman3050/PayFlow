package com.payflow.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryResponse {
    private Long walletId;
    private String type; // DEBIT or CREDIT
    private BigDecimal amount;
    private String currency;
}
