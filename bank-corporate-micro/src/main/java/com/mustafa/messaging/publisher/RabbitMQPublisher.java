package com.mustafa.messaging.publisher;

import com.mustafa.dto.message.NotificationMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    void configureReturns() {
        rabbitTemplate.setReturnsCallback(returned ->
                log.warn("Notification could not be routed; messageId={}",
                        returned.getMessage().getMessageProperties().getMessageId()));
    }

    public void sendNotification(NotificationMessage notification) {
        String messageId = UUID.randomUUID().toString();
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(notification, messageId);
                }
            });
        } else {
            publish(notification, messageId);
        }
    }

    private void publish(NotificationMessage notification, String messageId) {
        try {
            CorrelationData correlation = new CorrelationData(messageId);
            correlation.getFuture().whenComplete((confirm, failure) -> {
                if (failure != null || confirm == null || !confirm.isAck() || correlation.getReturned() != null) {
                    log.warn("Notification publication failed; messageId={}, type={}",
                            messageId, notification.getNotificationType());
                } else {
                    log.info("Notification accepted by broker; messageId={}, type={}",
                            messageId, notification.getNotificationType());
                }
            });
            rabbitTemplate.convertAndSend("bank_exchange", "notification_routing_key", notification,
                    message -> {
                        message.getMessageProperties().setMessageId(messageId);
                        return message;
                    }, correlation);
        } catch (RuntimeException exception) {
            log.warn("Notification publication failed; messageId={}, type={}, error={}",
                    messageId, notification.getNotificationType(), exception.getClass().getSimpleName());
        }
    }
}
