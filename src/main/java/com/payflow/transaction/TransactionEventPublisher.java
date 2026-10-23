package com.payflow.transaction;

import com.payflow.transaction.dto.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionCompletedEvent> kafkaTemplate;

    public void publishTransactionCompleted(Transaction transaction) {
        log.info("Publishing TransactionCompleted event for transaction: {}", transaction.getId());

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .fromWalletId(transaction.getFromWallet())
                .toWalletId(transaction.getToWallet())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .timestamp(transaction.getCreatedAt())
                .build();

        kafkaTemplate.send("transaction-completed", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction completed event for transaction {}:", transaction.getId(), ex);
                    } else {
                        log.info("Successfully published transaction completed event for transaction {}", transaction.getId());
                    }
                });
    }
}