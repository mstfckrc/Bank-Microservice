package com.mustafa.security;

import com.mustafa.client.AuthServiceClient;
import com.mustafa.dto.response.CustomerAccessContextResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CoreUserAccess {
    private final AuthServiceClient authServiceClient;

    public AuthorizationDecision authorizeUser(Supplier<? extends Authentication> authentication,
                                               RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken token)
                || !"bank-auth-client".equals(token.getToken().getClaimAsString("azp"))
                || !hasBusinessIdentity(token)) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(isCurrentApprovedUser(token, null));
    }

    public AuthorizationDecision authorizeAdmin(Supplier<? extends Authentication> authentication,
                                                RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken token)
                || !"bank-auth-client".equals(token.getToken().getClaimAsString("azp"))
                || !hasBusinessIdentity(token)) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(isCurrentApprovedUser(token, "ADMIN"));
    }

    public void requireBusinessAccess(String identityNumber, String expectedRole) {
        if (identityNumber == null || identityNumber.isBlank()) {
            throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_NOT_FOUND", HttpStatus.FORBIDDEN);
        }
        CustomerAccessContextResponse access = readAccessContext(identityNumber);
        requireApprovedAndUnrestricted(access);
        if (!expectedRole.equals(access.role())) {
            throw new BankAccessException("ACCOUNT_ROLE_CHANGED", HttpStatus.FORBIDDEN);
        }
    }

    private boolean isCurrentApprovedUser(JwtAuthenticationToken token, String expectedRole) {
        String identityNumber = token.getToken().getClaimAsString("bank_identity_number");
        CustomerAccessContextResponse access = readAccessContext(identityNumber);
        requireApprovedAndUnrestricted(access);

        String role = access.role();
        boolean roleMatchesToken = token.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
        if (!roleMatchesToken || (expectedRole != null && !expectedRole.equals(role))) {
            throw new BankAccessException("ACCOUNT_ROLE_CHANGED", HttpStatus.FORBIDDEN);
        }
        return true;
    }

    private CustomerAccessContextResponse readAccessContext(String identityNumber) {
        try {
            CustomerAccessContextResponse access = authServiceClient.getAccessContext(identityNumber);
            if (access == null || access.role() == null || access.approvalStatus() == null
                    || access.accessRestrictionStatus() == null) {
                throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return access;
        } catch (BankAccessException exception) {
            throw exception;
        } catch (FeignException.NotFound exception) {
            throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_NOT_FOUND", HttpStatus.FORBIDDEN);
        } catch (RuntimeException exception) {
            throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void requireApprovedAndUnrestricted(CustomerAccessContextResponse access) {
        if (!"APPROVED".equals(access.approvalStatus())) {
            throw new BankAccessException("ACCOUNT_NOT_APPROVED", HttpStatus.FORBIDDEN);
        }
        if (!"UNRESTRICTED".equals(access.accessRestrictionStatus())) {
            throw new BankAccessException("ACCOUNT_ACCESS_RESTRICTED", HttpStatus.FORBIDDEN);
        }
    }

    private boolean hasBusinessIdentity(JwtAuthenticationToken token) {
        String identityNumber = token.getToken().getClaimAsString("bank_identity_number");
        return identityNumber != null && !identityNumber.isBlank();
    }
}
