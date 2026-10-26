package com.mustafa.security;

import com.mustafa.client.AuthServiceClient;
import com.mustafa.dto.CustomerAccessContextResponse;
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
public class BillUserAccess {
    private final AuthServiceClient authServiceClient;

    public AuthorizationDecision authorizeRetailCustomer(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken token)
                || !"bank-auth-client".equals(token.getToken().getClaimAsString("azp"))) {
            return new AuthorizationDecision(false);
        }
        String identityNumber = token.getToken().getClaimAsString("bank_identity_number");
        if (identityNumber == null || identityNumber.isBlank()) return new AuthorizationDecision(false);
        requireRetailAccess(identityNumber);
        return new AuthorizationDecision(true);
    }

    public void requireRetailAccess(String identityNumber) {
        CustomerAccessContextResponse access;
        try {
            access = authServiceClient.getAccessContext(identityNumber);
            if (access == null || access.role() == null || access.approvalStatus() == null
                    || access.accessRestrictionStatus() == null) {
                throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (BankAccessException exception) {
            throw exception;
        } catch (FeignException.NotFound exception) {
            throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_NOT_FOUND", HttpStatus.FORBIDDEN);
        } catch (RuntimeException exception) {
            throw new BankAccessException("ACCOUNT_ACCESS_CONTEXT_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!"RETAIL_CUSTOMER".equals(access.role())) {
            throw new BankAccessException("ACCOUNT_ROLE_CHANGED", HttpStatus.FORBIDDEN);
        }
        if (!"APPROVED".equals(access.approvalStatus())) {
            throw new BankAccessException("ACCOUNT_NOT_APPROVED", HttpStatus.FORBIDDEN);
        }
        if (!"UNRESTRICTED".equals(access.accessRestrictionStatus())) {
            throw new BankAccessException("ACCOUNT_ACCESS_RESTRICTED", HttpStatus.FORBIDDEN);
        }
    }
}
