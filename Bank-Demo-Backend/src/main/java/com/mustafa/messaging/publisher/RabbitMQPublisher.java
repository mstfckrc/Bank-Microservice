package com.mustafa.messaging.publisher;

import com.mustafa.config.RabbitMQConfig;
import com.mustafa.dto.message.NotificationMessage; // 🚀 Eklendi
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 🚀 ARTIK SADECE RESMİ ZARF (DTO) KABUL EDİYORUZ!
     */
    public void sendNotification(NotificationMessage notification) {
        log.info("📢 Telsiz: Mesaj RabbitMQ postanesine fırlatılıyor... Tip: {}, Hedef: {}",
                notification.getNotificationType(), notification.getDestination());

        // Zarfı al, üzerine "bank_exchange" santralini ve "notification_routing_key" adresini yaz, fırlat!
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BANK_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                notification // Artık JSON olarak gidecek!
        );

        log.info("✅ Zarf başarıyla postaneyle teslim edildi! (Kullanıcı bekletilmiyor)");
    }
}