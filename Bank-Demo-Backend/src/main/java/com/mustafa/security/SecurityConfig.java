package com.mustafa.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CoreUserAccess coreUserAccess;
    private final InternalServiceAccess internalServiceAccess;
    private final Converter<Jwt, JwtAuthenticationToken> bankJwtAuthenticationConverter;

    @Bean
    @Order(0)
    SecurityFilterChain internalApi(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/internal", "/api/v1/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(bankJwtAuthenticationConverter)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/internal/accounts/customer/{identityNumber}")
                        .access((authentication, context) -> internalServiceAccess.authorize(
                                authentication, "bank-auth-service", "bank.customer.accounts.close"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/internal/accounts/validate")
                        .access((authentication, context) -> internalServiceAccess.authorize(
                                authentication, "bank-corporate-service", "bank.account.validate"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/transactions/bulk-salary")
                        .access((authentication, context) -> internalServiceAccess.authorize(
                                authentication, "bank-corporate-service", "bank.salary.pay"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/transactions/bill-payment")
                        .access((authentication, context) -> internalServiceAccess.authorize(
                                authentication, "bank-bill-service", "bank.bill.collect"))
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain publicAndInfrastructure(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/auth/**", "/api/v1/currencies/rates",
                        "/actuator/health", "/actuator/health/**", "/actuator/prometheus",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain userApi(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(bankJwtAuthenticationConverter)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/**").access(coreUserAccess::authorizeAdmin)
                        .anyRequest().access(coreUserAccess::authorizeUser));

        return http.build();
    }
}
