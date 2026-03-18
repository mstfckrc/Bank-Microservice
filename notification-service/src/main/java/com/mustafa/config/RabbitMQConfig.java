package com.mustafa.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nöbetçinin bekleyeceği kapı
    public static final String NOTIFICATION_QUEUE = "notification_queue";

    @Bean
    public Queue notificationQueue() {
        // True: Sunucu çökse bile bu kapı ve içindeki mesajlar silinmesin
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    // 🚀 EN KRİTİK ZIRH: Gelen JSON byte'larını, bizim NotificationMessage DTO'muza çeviren sihirbaz.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}