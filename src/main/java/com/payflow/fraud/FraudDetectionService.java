package com.payflow.fraud;

import com.payflow.transaction.Transaction;
import com.payflow.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Rule-based fraud engine. Runs inside the transfer's database transaction and
 * records a FraudFlag for each rule that fires; flags go to the admin review queue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    public static final String AMOUNT_THRESHOLD_EXCEEDED = "AMOUNT_THRESHOLD_EXCEEDED";
    public static final String VELOCITY_CHECK_FAILED = "VELOCITY_CHECK_FAILED";

    private final TransactionRepository transactionRepository;
    private final FraudFlagRepository fraudFlagRepository;

    @Value("${payflow.fraud.max-transfer-amount:10000.00}")
    private BigDecimal maxAmount;

    @Value("${payflow.fraud.velocity-window-minutes:5}")
    private long velocityWindowMinutes;

    @Value("${payflow.fraud.velocity-max-count:5}")
    private long velocityMaxCount;

    @Transactional
    public void checkFraudRules(Transaction transaction) {
        log.info("Checking fraud rules for transaction: {}", transaction.getId());

        // Rule 1: single transfer above the configured threshold
        if (transaction.getAmount().compareTo(maxAmount) > 0) {
            createFraudFlag(transaction, AMOUNT_THRESHOLD_EXCEEDED,
                    "Amount " + transaction.getAmount().stripTrailingZeros().toPlainString()
                            + " is above the " + maxAmount.stripTrailingZeros().toPlainString() + " limit");
        }

        // Rule 2: too many transfers from the same wallet inside a sliding window
        // (the count includes the transfer being checked)
        Instant windowStart = Instant.now().minus(velocityWindowMinutes, ChronoUnit.MINUTES);
        long recentTransfers = transactionRepository.countByFromWalletAndCreatedAtAfter(
                transaction.getFromWallet(), windowStart);
        if (recentTransfers > velocityMaxCount) {
            createFraudFlag(transaction, VELOCITY_CHECK_FAILED,
                    recentTransfers + " transfers from this wallet in the last " + velocityWindowMinutes + " minutes");
        }
    }

    private void createFraudFlag(Transaction transaction, String rule, String description) {
        FraudFlag flag = FraudFlag.builder()
                .transaction(transaction)
                .rule(rule)
                .description(description)
                .build();
        fraudFlagRepository.save(flag);
        log.warn("Fraud rule {} triggered for transaction {}", rule, transaction.getId());
    }
}
