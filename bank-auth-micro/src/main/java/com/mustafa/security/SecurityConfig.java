package com.mustafa.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // 🚀 YENİ MİMARİ: İç hat (Internal) telsizlerini dinleyecek filtremizi ekledik
    private final InternalHeaderFilter internalHeaderFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Herkese açık dış kapılar
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/currencies/rates").permitAll()

                        // Admin yetkileri gerektiren kapılar
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 🚀 İÇ KAPILAR: Dışarıdan JWT istenmez, ama içeride bizim InternalHeaderFilter'ımız TC'yi yakalayıp Context'e koyar!
                        .requestMatchers("/api/v1/internal/**").permitAll()

                        .requestMatchers("/error").permitAll() // 🚀 YENİ EKLENDİ: Hata sayfalarına izin ver ki 403 yemeyelim!

                        // Geri kalan tüm istekler için JWT token zorunlu
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 🛡️ ZIRHLARI GİYİYORUZ (Sıralama Önemlidir)
                // 1. Önce iç hattan gelen mikroservis telsizlerini (Header) kontrol et
                .addFilterBefore(internalHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. Sonra dışarıdan gelen normal kullanıcıların JWT'lerini kontrol et
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}