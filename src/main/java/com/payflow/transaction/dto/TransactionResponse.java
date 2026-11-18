package com.payflow.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String idempotencyKey;
    /** TRANSFER between two users, or TOP_UP funded by the system wallet. */
    private String kind;
    private Long fromWallet;
    private Long toWallet;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String failureReason;
    /** True while the transaction has an unreviewed fraud flag. */
    private boolean underReview;
    private Instant createdAt;

    /** Only filled in by GET /api/v1/transactions/{id}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<LedgerEntryResponse> ledgerEntries;
}
