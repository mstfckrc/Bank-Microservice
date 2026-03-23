package com.mustafa.config;

import com.mustafa.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    // 🚀 Polisi Karargaha Çağırıyoruz
    private final AuthenticationFilter authenticationFilter;

    public GatewayRoutingConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("java-monolith-route", r -> r.path("/api/v1/**")
                        // 🚀 DİKKAT: Artık kör kurye değiliz, her istek polisten geçecek!
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://backend:8080"))
                .build();
    }
}