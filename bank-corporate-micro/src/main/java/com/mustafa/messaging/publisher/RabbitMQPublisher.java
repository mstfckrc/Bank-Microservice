package com.mustafa.messaging.publisher;

import com.mustafa.dto.message.NotificationMessage; // Bu DTO'yu da ana projeden kopyaladığını varsayıyorum
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    // YML dosyasından veya default olarak exchange adını alıyoruz
    @Value("${rabbitmq.exchange.name:bank_exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.notification:notification_route}")
    private String notificationRoutingKey;

    public void sendNotification(NotificationMessage message) {
        log.info("RabbitMQ'ya bildirim mesajı fırlatılıyor. Hedef: {}", message.getDestination());
        rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, message);
    }
}