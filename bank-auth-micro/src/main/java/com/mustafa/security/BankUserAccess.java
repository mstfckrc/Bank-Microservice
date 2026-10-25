package com.mustafa.security;

import com.mustafa.entity.AppUser;
import com.mustafa.repository.IAppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class BankUserAccess {
    private final IAppUserRepository appUserRepository;

    public AuthorizationDecision authorizeUser(Supplier<? extends Authentication> authentication,
                                               RequestAuthorizationContext context) {
        return new AuthorizationDecision(findBoundUser(authentication.get()).isPresent());
    }

    public AuthorizationDecision authorizeAdmin(Supplier<? extends Authentication> authentication,
                                                RequestAuthorizationContext context) {
        return new AuthorizationDecision(findBoundUser(authentication.get())
                .filter(user -> user.getRole() == AppUser.Role.ADMIN).isPresent());
    }

    public AuthorizationDecision authorizeCorporateProfileRead(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        boolean allowed = auth instanceof JwtAuthenticationToken token
                && "bank-corporate-service".equals(token.getToken().getClaimAsString("azp"))
                && auth.getAuthorities().stream().anyMatch(authority ->
                        "SCOPE_bank.customer.profile.read".equals(authority.getAuthority()));
        return new AuthorizationDecision(allowed);
    }

    public AppUser currentUser() {
        return findBoundUser(SecurityContextHolder.getContext().getAuthentication())
                .orElseThrow(() -> new AccessDeniedException("Bank user mapping is unavailable."));
    }

    private Optional<AppUser> findBoundUser(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken token)
                || !"bank-auth-client".equals(token.getToken().getClaimAsString("azp"))) {
            return Optional.empty();
        }
        String identityNumber = token.getToken().getClaimAsString("bank_identity_number");
        if (identityNumber == null || identityNumber.isBlank()) return Optional.empty();
        return appUserRepository.findByKeycloakId(token.getToken().getSubject())
                .filter(user -> identityNumber.equals(user.getIdentityNumber()))
                .filter(user -> user.getRole() != null && auth.getAuthorities().stream()
                        .anyMatch(authority -> ("ROLE_" + user.getRole().name())
                                .equals(authority.getAuthority())));
    }
}
