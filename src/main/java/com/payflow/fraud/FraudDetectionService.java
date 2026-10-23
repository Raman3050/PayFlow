package com.payflow.fraud;

import com.payflow.transaction.Transaction;
import com.payflow.transaction.TransactionRepository;
import com.payflow.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Transactional
    public void checkFraudRules(Transaction transaction) {
        log.info("Checking fraud rules for transaction: {}", transaction.getId());

        // Rule 1: Check amount threshold
        BigDecimal maxAmount = BigDecimal.valueOf(10000.0); // Configurable
        if (transaction.getAmount().compareTo(maxAmount) > 0) {
            createFraudFlag(transaction, "AMOUNT_THRESHOLD_EXCEEDED", "Transaction amount exceeds threshold");
            log.warn("Fraud rule triggered for transaction {}: Amount threshold exceeded", transaction.getId());
        }

        // Rule 2: Check transfer velocity (more than 5 transactions in 5 minutes)
        Instant windowStart = Instant.now().minus(5, ChronoUnit.MINUTES);
        long transactionCount = transactionRepository.countByFromWalletOrToWallet(
                transaction.getFromWallet(), transaction.getToWallet());

        if (transactionCount > 5) {
            createFraudFlag(transaction, "VELOCITY_CHECK_FAILED", "Transfer velocity exceeds threshold");
            log.warn("Fraud rule triggered for transaction {}: Velocity check failed", transaction.getId());
        }
    }

    private void createFraudFlag(Transaction transaction, String rule, String description) {
        FraudFlag flag = FraudFlag.builder()
                .transaction(transaction)
                .rule(rule)
                .description(description)
                .build();

        transaction.setFraudFlag(flag);
        transactionRepository.save(transaction);
        log.info("Created fraud flag: {} for transaction {}", rule, transaction.getId());
    }
}