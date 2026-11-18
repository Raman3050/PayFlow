package com.payflow.transaction.dto;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreateLedgerEntryRequest {
    private Long transactionId;
    private Long walletId;
    private String type;
    private BigDecimal amount;
    private String currency;
}
