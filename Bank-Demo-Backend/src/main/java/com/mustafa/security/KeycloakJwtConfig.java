package com.mustafa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Configuration
public class KeycloakJwtConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${app.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> bankClaims = jwt -> {
            String clientId = jwt.getClaimAsString("azp");
            String identityNumber = jwt.getClaimAsString("bank_identity_number");
            boolean valid = jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getIssuedAt() != null && jwt.getExpiresAt() != null
                    && jwt.getAudience() != null && jwt.getAudience().contains(audience)
                    && "Bearer".equals(jwt.getClaimAsString("typ"))
                    && clientId != null && !clientId.isBlank()
                    && (!"bank-auth-client".equals(clientId)
                        || (identityNumber != null && !identityNumber.isBlank()));
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                            "invalid_token", "Required Bank access-token claims are missing or invalid.", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), bankClaims));
        return decoder;
    }

    @Bean
    Converter<Jwt, JwtAuthenticationToken> bankJwtAuthenticationConverter() {
        Set<String> applicationRoles = Set.of("ADMIN", "RETAIL_CUSTOMER", "CORPORATE_MANAGER");
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            Object rawRealmAccess = jwt.getClaims().get("realm_access");
            if (rawRealmAccess instanceof Map<?, ?> realmAccess
                    && realmAccess.get("roles") instanceof Collection<?> roles) {
                roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .filter(applicationRoles::contains)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }

            String identityNumber = jwt.getClaimAsString("bank_identity_number");
            String principalName = identityNumber == null || identityNumber.isBlank()
                    ? jwt.getSubject()
                    : identityNumber;
            return new JwtAuthenticationToken(jwt, authorities, principalName);
        };
    }
}
