package com.mustafa.filter; // Kendi paket adına göre ayarla

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            // İstek korumalı bir rotaya mı gidiyor? (VIP listede değilse)
            if (validator.isSecured.test(exchange.getRequest())) {

                // 1. Header'da Authorization (Bearer) var mı?
                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete(); // Kapıdan kov!
                }

                if (authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7); // "Bearer " kısmını kes at
                }

                try {
                    // 2. Mührü Kontrol Et (Sahteyse Exception fırlatır)
                    jwtUtil.validateToken(authHeader);

                    // 3. Kimlik Kartını Zımbala (Header Mutation)
                    Claims claims = jwtUtil.getClaims(authHeader);
                    String identityNumber = claims.getSubject(); // TC numarası
                    String role = claims.get("role", String.class); // Rolü

                    // İsteğin içine kendi mühürlü Header'larımızı basıp Backend'e yolluyoruz!
                    exchange = exchange.mutate().request(
                            exchange.getRequest().mutate()
                                    .header("X-User-TC", identityNumber)
                                    .header("X-User-Role", role)
                                    .build()
                    ).build();

                } catch (Exception e) {
                    System.out.println("🚨 Sınır İhlali! Geçersiz Token: " + e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete(); // Kapıdan kov!
                }
            }
            // Her şey yolundaysa kapıyı aç, geçsin
            return chain.filter(exchange);
        });
    }

    public static class Config {
    }
}