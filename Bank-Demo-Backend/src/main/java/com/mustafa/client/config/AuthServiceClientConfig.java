package com.mustafa.client.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

// Scoped to AuthServiceClient so the service credential cannot leak to other Feign calls.
public class AuthServiceClientConfig {
    @Bean
    public RequestInterceptor authServiceBearer(OAuth2AuthorizedClientManager manager) {
        return template -> {
            OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId("bank-service")
                    .principal("bank-core-service")
                    .build());
            if (client == null) {
                throw new IllegalStateException("Core service authorization is unavailable.");
            }
            template.removeHeader("Authorization");
            template.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }
}
