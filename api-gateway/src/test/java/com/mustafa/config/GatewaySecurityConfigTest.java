package com.mustafa.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(
        properties = "logging.config=classpath:logback-test.xml",
        controllers = GatewaySecurityConfigTest.ProbeController.class)
@ContextConfiguration(classes = GatewaySecurityConfigTest.TestApplication.class)
@Import({
        SecurityConfig.class,
        GatewaySecurityConfigTest.JwtDecoderConfiguration.class,
        GatewaySecurityConfigTest.ProbeController.class
})
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @SpringBootConfiguration
    static class TestApplication {
    }

    @Test
    void deniesUnauthenticatedInternalApiRequests() {
        webTestClient.get()
                .uri("/api/v1/internal/probe")
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri("/api/v1/internal")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deniesAuthenticatedUserJwtFromInternalApi() {
        webTestClient.get()
                .uri("/api/v1/internal/probe")
                .headers(headers -> headers.setBearerAuth("test-token"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deniesCorsPreflightForInternalApi() {
        webTestClient.options()
                .uri("/api/v1/internal/probe")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void continuesToAllowAuthenticatedUserOnProtectedPublicApi() {
        webTestClient.get()
                .uri("/api/v1/probe")
                .headers(headers -> headers.setBearerAuth("test-token"))
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("reached");
    }

    @Test
    void rejectsAnonymousProtectedApiRequests() {
        webTestClient.get()
                .uri("/api/v1/probe")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void keepsPublicCurrencyApiAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/api/v1/currencies/probe")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("reached");
    }

    @RestController
    public static class ProbeController {
        @GetMapping({
                "/api/v1/internal",
                "/api/v1/internal/probe",
                "/api/v1/probe",
                "/api/v1/currencies/probe"
        })
        String probe() {
            return "reached";
        }
    }

    @TestConfiguration
    static class JwtDecoderConfiguration {
        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("test-user")
                    .claim("realm_access", java.util.Map.of("roles", java.util.List.of("RETAIL_CUSTOMER")))
                    .build());
        }
    }
}
