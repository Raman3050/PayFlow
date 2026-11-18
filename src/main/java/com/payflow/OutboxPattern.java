package com.payflow;

import com.payflow.transaction.Transaction;
import com.payflow.transaction.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OutboxPattern {

    // This is an example of the Outbox Pattern
    // In a real implementation, you would have an outbox table and a separate process that reads from it
    // and publishes events to Kafka

    private final TransactionRepository transactionRepository;

    public OutboxPattern(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void saveTransactionWithEvent(Transaction transaction) {
        transactionRepository.save(transaction);
        log.info("Transaction saved with event: {}", transaction.getId());
    }
}