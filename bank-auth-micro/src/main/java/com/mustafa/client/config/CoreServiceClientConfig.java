package com.mustafa.client.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

// This configuration is intentionally scoped only to the Core Feign client.
public class CoreServiceClientConfig {

    @Bean
    RequestInterceptor coreServiceBearer(OAuth2AuthorizedClientManager manager) {
        return template -> {
            OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId("bank-service")
                    .principal("bank-auth-service")
                    .build());
            if (client == null) {
                throw new IllegalStateException("Auth service authorization is unavailable.");
            }
            template.removeHeader("Authorization");
            template.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }
}
