package com.mustafa.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FeignIdentityInterceptorTest {

    private final FeignIdentityInterceptor interceptor = new FeignIdentityInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsAuthenticatedIdentity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("11111111111", null, List.of()));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals("11111111111", template.headers().get("X-Identity-Number").iterator().next());
    }

    @Test
    void doesNotForwardIdentityForAnonymousRequest() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertFalse(template.headers().containsKey("X-Identity-Number"));
    }
}
