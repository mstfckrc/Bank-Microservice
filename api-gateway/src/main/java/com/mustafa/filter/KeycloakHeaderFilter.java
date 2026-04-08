package com.mustafa.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class KeycloakHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                        Jwt jwt = (Jwt) authentication.getPrincipal();

                        // 1. TC'yi al
                        String identityNumber = jwt.getClaimAsString("preferred_username");

                        // 2. Rolleri al ("ROLE_ADMIN", "ROLE_CUSTOMER" vb.)
                        String roles = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","));

                        // 🚀 X-RAY LOGLAR: Gateway'in arkaya ne fısıldadığını Terminal'de görelim!
                        System.out.println("\n🎯 [SÜPER GATEWAY] Karargaha İstek Yönlendiriliyor!");
                        System.out.println("-> HEDEF URL: " + exchange.getRequest().getURI());
                        System.out.println("-> TC KİMLİK: " + identityNumber);
                        System.out.println("-> İLETİLEN ROLLER: " + roles);
                        System.out.println("--------------------------------------------------\n");

                        // 3. İsteği değiştir ve arkaya yolla
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .headers(headers -> {
                                    // Bizi ilgilendiren yeni başlıkları ekle
                                    headers.set("X-Identity-Number", identityNumber);
                                    headers.set("X-User-Role", roles);
                                    // 🚀 KRİTİK: Eski Keycloak JWT biletini ÇÖPE AT!
                                    // Arkadaki servis sadece X-Identity-Number'ı görsün.
                                    headers.remove("Authorization");
                                })
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -1; // Yetki kontrolünden hemen sonra çalışsın
    }
}