package com.mustafa.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    // Radarımızı buraya çağırıyoruz
    private final RateLimitConfig rateLimitConfig;

    public GatewayRoutingConfig(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    /**
     * 🛡️ ÖZEL JETON KOVASI (Siber Kalkan Mühimmatı)
     * Saniyede 1 jeton dolar, kova en fazla 3 jeton alır.
     */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(1, 3);
    }

    // 🗺️ KUSURSUZ HARİTA MERKEZİ (V6.0 - RATE LIMIT DEVRİMİ)
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1. ÖNCELİK: Döviz Servisi
                .route("currency-service-route", r -> r.path("/api/v1/currencies/**")
                        .uri("http://currency-service:8083"))

                // 2. ÖNCELİK: Fatura Servisi
                .route("bill-service-route", r -> r.path("/api/v1/bills/**")
                        .uri("http://bill-service:8084"))

                // 3. ÖNCELİK: Kurumsal Yönetim
                .route("corporate-service-route", r -> r.path("/api/v1/companies/employees/**")
                        .uri("http://corporate-service:8085"))

                // 4. Admin'in Müşteri Hesaplarına Erişimi
                .route("backend-admin-customer-accounts-route", r -> r.path("/api/v1/admin/customers/*/accounts")
                        .uri("http://backend:8080"))

                // 5. Admin'in Müşteri Yönetimi
                .route("auth-admin-route", r -> r.path("/api/v1/admin/customers/**")
                        .uri("http://auth-service:8086"))

                // ==========================================
                // 6-A: 🛡️ SİBER KALKANLI AUTH ROTASI (Login/Register)
                // ==========================================
                .route("auth-service-login-route", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.requestRateLimiter(config -> {
                            config.setRateLimiter(authRateLimiter());
                            config.setKeyResolver(rateLimitConfig.ipKeyResolver());
                        }))
                        .uri("http://auth-service:8086"))

                // ==========================================
                // 6-B: 🔓 SERBEST MÜŞTERİ PROFİL ROTASI
                // ==========================================
                .route("customer-profile-route", r -> r.path("/api/v1/customers/**")
                        .uri("http://auth-service:8086"))

                // 7. KARARGAH: Geri kalan her şey
                .route("java-monolith-route", r -> r.path("/api/v1/**")
                        .uri("http://backend:8080"))
                .build();
    }
}