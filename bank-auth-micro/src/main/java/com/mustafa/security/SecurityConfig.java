package com.mustafa.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final BankUserAccess bankUserAccess;
    private final JwtAuthenticationConverter bankJwtAuthenticationConverter;

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
                        .requestMatchers(HttpMethod.GET, "/api/v1/internal/users/profile/*")
                        .access(bankUserAccess::authorizeCorporateProfileRead)
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain publicApi(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/auth/register", "/actuator/health", "/actuator/health/**",
                        "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        // Keep existing internal health, metrics and documentation consumers working.
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
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(bankJwtAuthenticationConverter)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/**").access(bankUserAccess::authorizeAdmin)
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/profile")
                        .access(bankUserAccess::authorizeUser)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customers/profile")
                        .access(bankUserAccess::authorizeUser)
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers/appeal")
                        .access(bankUserAccess::authorizeUser)
                        .anyRequest().denyAll());
        return http.build();
    }
}
