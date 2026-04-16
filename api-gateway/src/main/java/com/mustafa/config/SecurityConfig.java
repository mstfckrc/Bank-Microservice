package com.mustafa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // -------------------------------------------------------------------
    // 🟢 1. KAPI: SİVİL BÖLGE (Öncelik 1)
    // Sadece OPTIONS istekleri, Auth işlemleri, Kurlar ve Eureka buraya düşer.
    // -------------------------------------------------------------------
    @Bean
    @Order(1)
    public SecurityWebFilterChain publicFilterChain(ServerHttpSecurity http) {
        http
                // Hata veren kısmı ServerWebExchangeMatchers fabrikası ile çözdük:
                .securityMatcher(new OrServerWebExchangeMatcher(
                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.OPTIONS, "/**"), // CORS Preflight için serbest geçiş
                        ServerWebExchangeMatchers.pathMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/currencies/**",
                                "/eureka/**"
                        )
                ))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        // DİKKAT: .oauth2ResourceServer() BURADA YOK. Bu sayede ölü token hata verdirmez.

        return http.build();
    }

    // -------------------------------------------------------------------
    // 🔴 2. KAPI: ASKERİ BÖLGE (Öncelik 2 - Varsayılan Kapı)
    // Sivil olmayan her istek (Transferler, Profiller vb.) buraya düşer.
    // -------------------------------------------------------------------
    @Bean
    @Order(2)
    public SecurityWebFilterChain securedFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
                // Token kontrolü ve Keycloak Tercümanı burada devreye girer
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                );

        return http.build();
    }

    // 🕵️‍♂️ KEYCLOAK TERCÜMANI (Translator)
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            // Eğer kasa yoksa veya içi boşsa yetkisiz (boş liste) dön
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Collections.emptyList();
            }

            // Kasadaki rolleri al ("ADMIN", "RETAIL_CUSTOMER" vb.)
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            // Başlarına "ROLE_" ekleyerek Spring'in eline ver ("ROLE_ADMIN")
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}