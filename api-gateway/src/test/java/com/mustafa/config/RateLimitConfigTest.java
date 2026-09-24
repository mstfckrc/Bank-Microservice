package com.mustafa.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitConfigTest {

    @Test
    void usesRemoteAddressWhenForwardedHeaderIsAbsent() {
        var request = MockServerHttpRequest.get("/api/v1/auth/register")
                .remoteAddress(InetSocketAddress.createUnresolved("192.0.2.10", 1234))
                .build();
        var exchange = MockServerWebExchange.from(request);

        String key = new RateLimitConfig().ipKeyResolver().resolve(exchange).block();

        assertEquals("192.0.2.10", key);
    }
}
