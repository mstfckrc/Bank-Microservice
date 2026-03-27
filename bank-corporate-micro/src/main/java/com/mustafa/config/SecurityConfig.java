package com.mustafa.config;

import com.mustafa.security.HeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Mikroservisler arası iletişimde CSRF kapalı olmalı
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT/Header mantığı stateless'tır
                .authorizeHttpRequests(auth -> auth
                        // Tüm kapıları açıyoruz, çünkü asıl koruma duvarımız (API Gateway) dışarıda duruyor!
                        .anyRequest().permitAll()
                )
                // Bizim yazdığımız o hafif yaka kartı okuyucusunu, Spring'in ağır filtrelerinden önce araya sokuyoruz
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}