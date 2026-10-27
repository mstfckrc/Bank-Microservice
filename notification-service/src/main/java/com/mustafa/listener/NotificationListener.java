package com.mustafa.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mustafa.config.RabbitMQConfig;
import com.mustafa.dto.message.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE, messageConverter = "rawNotificationMessageConverter")
    public void receiveNotification(byte[] body, Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        NotificationMessage notification;
        try {
            notification = MAPPER.readValue(body, NotificationMessage.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid notification JSON");
        }
        if (notification == null || notification.getNotificationType() == null) {
            throw new IllegalArgumentException("Notification type is missing");
        }

        switch (notification.getNotificationType()) {
            case SMS, EMAIL, PUSH_NOTIFICATION, SYSTEM_ALERT ->
                    log.info("Notification processing simulated; messageId={}, type={}",
                            messageId, notification.getNotificationType());
            default -> throw new IllegalArgumentException("Unsupported notification type");
        }
    }
}
