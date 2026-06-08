package com.mustafa.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // 1. KUBERNETES / NGINX SENARYOSU: Nginx'in araya eklediği asıl IP başlığını ara.
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                // Eğer proxy arkasındaysak ve başlık geldiyse (virgüllü olabilir), ilk IP'yi al.
                return Mono.just(forwardedFor.split(",")[0].trim());
            }

            // 2. DOCKER COMPOSE SENARYOSU: Doğrudan bağlantı var, proxy yok.
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null) {
                // DİKKAT: NPE yememek için getAddress().getHostAddress() YERİNE getHostString() KULLANIYORUZ!
                return Mono.just(remoteAddress.getHostString());
            }

            // 3. HİÇBİRİ YOKSA: Kalkanı boş geçirmemek için bilinmeyen olarak işaretle.
            return Mono.just("unknown");
        };
    }

    // 🚀 SİBER KALKANIN KENDİSİ
    // Spring Boot bunu alacak, içine Redis ayarlarını gömecek ve Gateway'e verecek.
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 2);
    }
}