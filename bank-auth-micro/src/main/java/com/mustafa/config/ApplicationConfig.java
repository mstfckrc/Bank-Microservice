package com.mustafa.config;

import com.mustafa.repository.IAppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final IAppUserRepository appUserRepository;

    // Spring Security'ye kullanıcıyı (Müşteriyi) TC No ile bulmasını söylüyoruz
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> appUserRepository.findByIdentityNumber(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Kullanıcı bulunamadı: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Constructor (yapıcı metot) kullanımı en garantisi
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}