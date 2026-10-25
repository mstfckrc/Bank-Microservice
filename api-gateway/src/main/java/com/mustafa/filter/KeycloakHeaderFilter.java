package com.mustafa.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class KeycloakHeaderFilter implements GlobalFilter, Ordered {

    private static final String IDENTITY_NUMBER_HEADER = "X-Identity-Number";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String INTERNAL_IDENTITY_HEADER = "X-Internal-Identity";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(IDENTITY_NUMBER_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(INTERNAL_IDENTITY_HEADER);
                    headers.remove("X-User-TC");
                    // Preserve Authorization: each downstream Resource Server verifies the JWT itself.
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1; // Yetki kontrolünden hemen sonra çalışsın
    }
}
