package com.mustafa.config; // Kendi paket adına göre düzenle

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 1. KUYRUK ADI: Bankadaki bildirimlerin (Mail/SMS) bekleyeceği gişe
    public static final String NOTIFICATION_QUEUE = "notification_queue";

    // 2. SANTRAL (EXCHANGE) ADI: Mesajları yönlendiren ana merkez
    public static final String BANK_EXCHANGE = "bank_exchange";

    // 3. YÖNLENDİRME ETİKETİ (ROUTING KEY): Zarfın üzerindeki adres
    public static final String NOTIFICATION_ROUTING_KEY = "notification_routing_key";

    // --- BİNAYI İNŞA EDİYORUZ ---

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true); // true = Sunucu çökse bile mesajlar silinmesin!
    }

    @Bean
    public DirectExchange bankExchange() {
        return new DirectExchange(BANK_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange bankExchange) {
        // Zarfın üzerinde "notification_routing_key" yazıyorsa, bunu "notification_queue" gişesine yönlendir diyoruz.
        return BindingBuilder.bind(notificationQueue).to(bankExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // --- JSON ÇEVİRİCİ (EN KRİTİK NOKTA) ---
    // Spring Boot varsayılan olarak nesneleri karmaşık Java byte'larına çevirir.
    // Biz JSON'a çevirmesini söylüyoruz ki, Kibana'da veya başka dillerde rahatça okuyabilelim.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}