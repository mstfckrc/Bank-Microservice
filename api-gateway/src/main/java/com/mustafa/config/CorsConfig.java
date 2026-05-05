package com.mustafa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

	    corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));

        // Bütün HTTP taktiklerine (GET, POST, OPTIONS vb.) izin veriyoruz
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Pasaport ve Mühürlere (Headers) tam izin
        corsConfig.setAllowedHeaders(Arrays.asList("*"));

        // Token (Bearer) ve Cookie taşınabilmesi için kritik izin
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Bütün rotalar (/**) için bu CORS kalkanını uygula
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
