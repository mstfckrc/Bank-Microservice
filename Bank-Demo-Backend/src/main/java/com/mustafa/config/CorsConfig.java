package com.mustafa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // React'ın çalışacağı olası portlara izin veriyoruz
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true); // Token'ların (Bearer) geçmesine izin verir

        source.registerCorsConfiguration("/**", config); // Tüm uç noktalarımız için geçerli
        return new CorsFilter(source);
    }
}