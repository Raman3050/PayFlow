package com.payflow.transaction;

import com.payflow.transaction.dto.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionCompletedEvent> kafkaTemplate;

    /** Set KAFKA_ENABLED=false on hosts without a broker (e.g. a free Render instance). */
    @Value("${payflow.events.kafka-enabled:true}")
    private boolean kafkaEnabled;

    /**
     * Publishes the event only after the database transaction commits, so consumers
     * never hear about a transfer that was rolled back.
     */
    public void publishTransactionCompleted(Transaction transaction) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .fromWalletId(transaction.getFromWallet())
                .toWalletId(transaction.getToWallet())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .timestamp(transaction.getCreatedAt())
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(TransactionCompletedEvent event) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled; skipping TransactionCompleted event for transaction {}", event.getTransactionId());
            return;
        }
        try {
            kafkaTemplate.send("transaction-completed", String.valueOf(event.getTransactionId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event for transaction {}", event.getTransactionId(), ex);
                        } else {
                            log.info("Published event for transaction {}", event.getTransactionId());
                        }
                    });
        } catch (Exception ex) {
            // The money has already moved; a broker problem must not fail the API call
            log.error("Could not publish event for transaction {}", event.getTransactionId(), ex);
        }
    }
}
