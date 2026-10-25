package com.mustafa.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class CorporateAccess {

    public AuthorizationDecision authorizeCorporateManager(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            return new AuthorizationDecision(false);
        }

        String identityNumber = token.getToken().getClaimAsString("bank_identity_number");
        boolean allowed = "bank-auth-client".equals(token.getToken().getClaimAsString("azp"))
                && identityNumber != null && !identityNumber.isBlank()
                && auth.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_CORPORATE_MANAGER".equals(authority.getAuthority()));
        return new AuthorizationDecision(allowed);
    }

    public AuthorizationDecision authorizeServiceScope(
            Supplier<? extends Authentication> authentication,
            String expectedClient,
            String requiredScope) {
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
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (rawScope instanceof Collection<?> scopes) {
            return scopes.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
