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

    // 🗺️ KUSURSUZ HARİTA MERKEZİ (CORS işini CorsConfig.java'ya bıraktık)
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

                // 3. KARARGAH: Geri kalan her şey (Auth, Login vs.)
                .route("java-monolith-route", r -> r.path("/api/v1/**")
                        .filters(f -> f.filter(authenticationFilter.apply(filterConfig)))
                        .uri("http://backend:8080"))
                .build();
    }
}