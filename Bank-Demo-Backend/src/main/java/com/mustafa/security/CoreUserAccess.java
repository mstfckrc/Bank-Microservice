package com.mustafa.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class CoreUserAccess {

    public AuthorizationDecision authorizeUser(Supplier<? extends Authentication> authentication,
                                               RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        boolean allowed = auth instanceof JwtAuthenticationToken token
                && "bank-auth-client".equals(token.getToken().getClaimAsString("azp"))
                && hasBusinessIdentity(token);
        return new AuthorizationDecision(allowed);
    }

    public AuthorizationDecision authorizeAdmin(Supplier<? extends Authentication> authentication,
                                                RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        boolean allowed = auth instanceof JwtAuthenticationToken token
                && "bank-auth-client".equals(token.getToken().getClaimAsString("azp"))
                && hasBusinessIdentity(token)
                && auth.getAuthorities().stream().anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority()));
        return new AuthorizationDecision(allowed);
    }

    private boolean hasBusinessIdentity(JwtAuthenticationToken token) {
        String identityNumber = token.getToken().getClaimAsString("bank_identity_number");
        return identityNumber != null && !identityNumber.isBlank();
    }
}
