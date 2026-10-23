package com.mustafa.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class KeycloakHeaderFilter implements GlobalFilter, Ordered {

    private static final String IDENTITY_NUMBER_HEADER = "X-Identity-Number";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String INTERNAL_IDENTITY_HEADER = "X-Internal-Identity";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Mono<AuthenticatedPrincipal> authenticatedPrincipal = ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> Mono.justOrEmpty(securityContext.getAuthentication()))
                .filter(authentication -> authentication.getPrincipal() instanceof Jwt)
                .map(authentication -> new AuthenticatedPrincipal(
                        (Jwt) authentication.getPrincipal(),
                        authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","))));

        return authenticatedPrincipal
                .map(principal -> mutateExchange(exchange, principal.jwt(), principal.roles()))
                .defaultIfEmpty(mutateExchange(exchange, null, null))
                .flatMap(chain::filter);
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, Jwt jwt, String roles) {
        String identityNumber = jwt == null ? null : jwt.getClaimAsString("preferred_username");

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(IDENTITY_NUMBER_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(INTERNAL_IDENTITY_HEADER);
                    headers.remove("X-User-TC"); // Core/Auth'taki legacy filter'ın alternatif kimlik girdisi.

                    if (identityNumber != null && !identityNumber.isBlank()) {
                        headers.set(IDENTITY_NUMBER_HEADER, identityNumber);
                    }
                    if (roles != null && !roles.isBlank()) {
                        headers.set(USER_ROLE_HEADER, roles);
                    }
                    if (jwt != null) {
                        headers.remove("Authorization");
                    }
                })
                .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    private record AuthenticatedPrincipal(Jwt jwt, String roles) {
    }

    @Override
    public int getOrder() {
        return -1; // Yetki kontrolünden hemen sonra çalışsın
    }
}
