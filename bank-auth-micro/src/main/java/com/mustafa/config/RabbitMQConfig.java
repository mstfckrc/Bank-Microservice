package com.mustafa.config;

import org.springframework.amqp.core.*;
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

    // --- DEFANSİF MİMARİ: Bildirim servisi kapalı olsa bile mektuplar kaybolmasın diye gişeyi biz de inşa ediyoruz ---

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
        return BindingBuilder.bind(notificationQueue).to(bankExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    // --- JSON ÇEVİRİCİ (EN KRİTİK NOKTA) ---
    // Auth servisimiz (Publisher) mesajları fırlatırken byte yerine temiz bir JSON'a çevirsin
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}