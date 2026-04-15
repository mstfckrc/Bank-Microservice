package com.mustafa.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1. Döviz Servisi
                .route("currency-service-route", r -> r.path("/api/v1/currencies/**")
                        .uri("http://currency-service:8083"))

                // 2. Fatura Servisi
                .route("bill-service-route", r -> r.path("/api/v1/bills/**")
                        .uri("http://bill-service:8084"))

                // 3. Kurumsal Yönetim
                .route("corporate-service-route", r -> r.path("/api/v1/companies/employees/**")
                        .uri("http://corporate-service:8085"))

                // 4. Admin -> Backend
                .route("backend-admin-customer-accounts-route", r -> r.path("/api/v1/admin/customers/*/accounts")
                        .uri("http://backend:8080"))

                // 5. Admin -> Auth
                .route("auth-admin-route", r -> r.path("/api/v1/admin/customers/**")
                        .uri("http://auth-service:8086"))

                // 6. KİMLİK ÜSSÜ (Kayıt ve Bireysel Profil - FİLTREDEN ARINDIRILDI ✅)
                .route("auth-service-route", r -> r.path("/api/v1/auth/**", "/api/v1/customers/**")
                        .uri("http://auth-service:8086"))

                // 7. KARARGAH: Geri kalan her şey (TRANSFERLER DAHİL, DÜZ GEÇİŞ)
                .route("java-monolith-route", r -> r.path("/api/v1/**")
                        .uri("http://backend:8080"))
                .build();
    }
}