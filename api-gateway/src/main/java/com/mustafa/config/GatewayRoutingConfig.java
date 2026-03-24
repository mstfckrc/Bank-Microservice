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

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 🚀 1. YENİ ROTA: Kur Servisi (Alt çizgi yok, tire var!)
                .route("currency-service-route", r -> r.path("/api/v1/currencies/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://currency-service:8083"))

                // 🚀 2. ESKİ ROTA: Monolit Karargah (Eski çalışan orijinal hali)
                .route("java-monolith-route", r -> r.path("/api/v1/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://backend:8080"))
                .build();
    }
}