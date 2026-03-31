package com.mustafa.config;

import com.mustafa.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    private final AuthenticationFilter authenticationFilter;

    public GatewayRoutingConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    // 🗺️ KUSURSUZ HARİTA MERKEZİ (V5.2 - SAF DOCKER STANDARDI VE ADMIN YÖNLENDİRMESİ)
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        AuthenticationFilter.Config filterConfig = new AuthenticationFilter.Config();

        return builder.routes()
                // 1. ÖNCELİK: Döviz Servisi
                .route("currency-service-route", r -> r.path("/api/v1/currencies/**")
                        .uri("http://currency-service:8083"))

                // 2. ÖNCELİK: Fatura Servisi
                .route("bill-service-route", r -> r.path("/api/v1/bills/**")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://bill-service:8084"))

                // 3. ÖNCELİK: Kurumsal Yönetim
                .route("corporate-service-route", r -> r.path("/api/v1/companies/employees/**")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://corporate-service:8085"))

                // 🚀 4. YENİ ÖNCELİK: Admin'in Müşteri Yönetimi (Kimlik Üssüne Gider)
                // 🚀 EKSİK OLAN HAYAT KURTARICI ROTA: Müşterinin hesapları KARARGAHA gitmeli!
                .route("backend-admin-customer-accounts-route", r -> r.path("/api/v1/admin/customers/*/accounts")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://backend:8080"))

                // Admin'in Müşteri Yönetimi (Kimlik Üssüne Gider)
                .route("auth-admin-route", r -> r.path("/api/v1/admin/customers/**")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://auth-service:8086"))

                // 5. KİMLİK ÜSSÜ (Kayıt, Giriş ve Bireysel Müşteri Profili)
                .route("auth-service-route", r -> r.path("/api/v1/auth/**", "/api/v1/customers/**")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://auth-service:8086"))

                // 6. KARARGAH: Geri kalan her şey (Accounts, Transactions, Admin'in Kasa işlemleri vs.)
                // (DİKKAT: Bu her zaman en altta kalmalı ki üsttekilerle eşleşmeyen her şey buraya düşsün)
                .route("java-monolith-route", r -> r.path("/api/v1/**")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://backend:8080"))
                .build();
    }
}