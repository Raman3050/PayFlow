package com.payflow.notification;

import com.payflow.transaction.dto.TransactionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = "transaction-completed", groupId = "payflow-group")
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Received transaction completed event: transactionId = {}, amount = {}, from = {}, to = {}",
                event.getTransactionId(),
                event.getAmount(),
                event.getFromWalletId(),
                event.getToWalletId());

        String notificationMessage = String.format(
                "Transaction %d completed: %.2f %s transferred from wallet %d to wallet %d",
                event.getTransactionId(),
                event.getAmount(),
                event.getCurrency(),
                event.getFromWalletId(),
                event.getToWalletId()
        );

        log.info("Notification: {}", notificationMessage);
    }
}