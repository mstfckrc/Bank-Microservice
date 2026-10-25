package com.mustafa.client.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

// Deliberately not a component-scanned @Configuration: only the Auth Feign client uses it.
public class AuthServiceClientConfig {
    @Bean
    RequestInterceptor authServiceBearer(OAuth2AuthorizedClientManager manager) {
        return template -> {
            OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId("bank-service")
                    .principal("bank-corporate-service")
                    .build());
            if (client == null) {
                throw new IllegalStateException("Corporate service authorization is unavailable.");
            }
            template.removeHeader("Authorization");
            template.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }
}
