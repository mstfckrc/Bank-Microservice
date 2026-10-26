package com.mustafa.config;

import com.mustafa.security.CorporateAccess;
import com.mustafa.security.BankAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorporateAccess corporateAccess;
    private final BankAccessDeniedHandler bankAccessDeniedHandler;
    private final Converter<Jwt, AbstractAuthenticationToken> corporateJwtAuthenticationConverter;

    @Bean
    @Order(0)
    SecurityFilterChain internalApi(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/internal", "/api/v1/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(corporateJwtAuthenticationConverter)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/companies/sync")
                        .access((authentication, context) -> corporateAccess.authorizeServiceScope(
                                authentication, "bank-auth-service", "bank.company.write"))
                        .requestMatchers(HttpMethod.PUT, "/api/v1/internal/companies/sync/*")
                        .access((authentication, context) -> corporateAccess.authorizeServiceScope(
                                authentication, "bank-auth-service", "bank.company.write"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/internal/companies/sync/*")
                        .access((authentication, context) -> corporateAccess.authorizeServiceScope(
                                authentication, "bank-auth-service", "bank.company.read"))
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/internal/companies/sync/*")
                        .access((authentication, context) -> corporateAccess.authorizeServiceScope(
                                authentication, "bank-auth-service", "bank.company.delete"))
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain publicApi(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/health", "/actuator/health/**", "/actuator/prometheus",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                                "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain userApi(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(bankAccessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(corporateJwtAuthenticationConverter)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/companies/employees",
                                "/api/v1/companies/employees/**")
                        .access(corporateAccess::authorizeCorporateManager)
                        .anyRequest().denyAll());
        return http.build();
    }
}
