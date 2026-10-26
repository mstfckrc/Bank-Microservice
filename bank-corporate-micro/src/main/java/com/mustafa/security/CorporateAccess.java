package com.mustafa.security;

import com.mustafa.client.IAuthServiceClient;
import com.mustafa.dto.response.CustomerAccessContextResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequiredArgsConstructor
public class CorporateAccess {
    private final IAuthServiceClient authServiceClient;

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
        if (!allowed) return new AuthorizationDecision(false);

        CustomerAccessContextResponse access = readAccessContext(identityNumber);
        requireApprovedAndUnrestricted(access);
        if (!"CORPORATE_MANAGER".equals(access.role())) {
            throw new BankAccessException("ACCOUNT_ROLE_CHANGED", HttpStatus.FORBIDDEN);
        }
        return new AuthorizationDecision(true);
    }

    public void requireCompanyAccess(String identityNumber) {
        CustomerAccessContextResponse access = readAccessContext(identityNumber);
        requireApprovedAndUnrestricted(access);
        if (!"CORPORATE_MANAGER".equals(access.role())) {
            throw new BankAccessException("ACCOUNT_ROLE_CHANGED", HttpStatus.FORBIDDEN);
        }
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
