package com.mustafa.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeycloakHeaderFilterTest {

    private final KeycloakHeaderFilter filter = new KeycloakHeaderFilter();

    @Test
    void authenticatedJwtReplacesClientIdentityHeadersAndDispatchesOnce() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts")
                        .header("Authorization", "Bearer client-token")
                        .header("X-Identity-Number", "spoofed-identity")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-Internal-Identity", "spoofed-internal-identity")
                        .build());
        Jwt jwt = Jwt.withTokenValue("verified-token")
                .header("alg", "none")
                .claim("preferred_username", "12345678901")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(
                new SimpleGrantedAuthority("ROLE_RETAIL_CUSTOMER")));

        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = forwarded -> {
            calls.incrementAndGet();
            forwardedExchange.set(forwarded);
            return Mono.empty();
        };

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertEquals(1, calls.get());
        ServerHttpRequest forwardedRequest = forwardedExchange.get().getRequest();
        assertEquals("12345678901", forwardedRequest.getHeaders().getFirst("X-Identity-Number"));
        assertEquals("ROLE_RETAIL_CUSTOMER", forwardedRequest.getHeaders().getFirst("X-User-Role"));
        assertNull(forwardedRequest.getHeaders().getFirst("X-Internal-Identity"));
        assertNull(forwardedRequest.getHeaders().getFirst("Authorization"));
    }

    @Test
    void anonymousRequestStripsSpoofedIdentityHeadersAndDispatchesOnce() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/currencies")
                        .header("X-Identity-Number", "spoofed-identity")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-Internal-Identity", "spoofed-internal-identity")
                        .build());

        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = forwarded -> {
            calls.incrementAndGet();
            forwardedExchange.set(forwarded);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(1, calls.get());
        assertNull(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Identity-Number"));
        assertNull(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Role"));
        assertNull(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Internal-Identity"));
    }

    @Test
    void downstreamFailureIsPropagatedWithoutRedispatch() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts").build());
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException expectedFailure = new IllegalStateException("downstream failure");
        GatewayFilterChain chain = forwarded -> {
            calls.incrementAndGet();
            return Mono.error(expectedFailure);
        };

        IllegalStateException actualFailure = assertThrows(IllegalStateException.class,
                () -> filter.filter(exchange, chain).block());

        assertSame(expectedFailure, actualFailure);
        assertEquals(1, calls.get());
    }
}
