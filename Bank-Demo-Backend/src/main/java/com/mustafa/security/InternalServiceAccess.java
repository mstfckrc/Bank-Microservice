package com.mustafa.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class InternalServiceAccess {

    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
                                           String expectedClient, String requiredScope) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            return new AuthorizationDecision(false);
        }

        boolean allowed = expectedClient.equals(token.getToken().getClaimAsString("azp"))
                && scopes(token).contains(requiredScope);
        return new AuthorizationDecision(allowed);
    }

    private Set<String> scopes(JwtAuthenticationToken token) {
        Object rawScope = token.getToken().getClaims().get("scope");
        if (rawScope instanceof String scope) {
            return Arrays.stream(scope.trim().split("\\s+"))
                    .filter(value -> !value.isBlank())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        if (rawScope instanceof Collection<?> scopes) {
            return scopes.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
