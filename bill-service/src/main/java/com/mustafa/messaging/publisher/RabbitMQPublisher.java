package com.mustafa.messaging.publisher;

import com.mustafa.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void sendNotification(NotificationMessage message) {
        // Karargahtaki (monolitteki) exchange ve routing key değerleri ile aynı
        rabbitTemplate.convertAndSend("bank_exchange", "notification_routing_key", message);
        log.info("🕊️ RabbitMQ: Bildirim kuyruğa fırlatıldı! Hedef Kimlik: {}", message.getIdentityNumber());
    }
}