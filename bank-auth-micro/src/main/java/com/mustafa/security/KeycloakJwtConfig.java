package com.mustafa.security;

import com.mustafa.entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class KeycloakJwtConfig {
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${app.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> bankClaims = jwt -> {
            String clientId = jwt.getClaimAsString("azp");
            Object businessIdentity = jwt.getClaims().get("bank_identity_number");
            boolean valid = jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                    && jwt.getAudience() != null && jwt.getAudience().contains(audience)
                    && "Bearer".equals(jwt.getClaimAsString("typ"))
                    && clientId != null && !clientId.isBlank()
                    && (!"bank-auth-client".equals(clientId)
                        || (businessIdentity instanceof String identity && !identity.isBlank()));
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                            "invalid_token", "Required Bank access-token claims are missing or invalid.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), bankClaims));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter bankJwtAuthenticationConverter() {
        Set<String> applicationRoles = Arrays.stream(AppUser.Role.values())
                .map(Enum::name).collect(Collectors.toSet());
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            Object rawRealmAccess = jwt.getClaims().get("realm_access");
            if (rawRealmAccess instanceof Map<?, ?> realmAccess
                    && realmAccess.get("roles") instanceof Collection<?> roles) {
                roles.stream().filter(String.class::isInstance).map(String.class::cast)
                        .filter(applicationRoles::contains)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }
            return authorities;
        });
        // The authenticated name stays sub; business identity is a separate claim.
        return converter;
    }
}
