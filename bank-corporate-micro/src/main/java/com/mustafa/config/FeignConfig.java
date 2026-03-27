package com.mustafa.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestIdentityInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                // Kurumsal Servisin bildiği kimliği (Senin TC'ni) al
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth != null && auth.getName() != null) {
                    // Karargaha "İç Hat Kimliği" olarak direkt fırlat!
                    requestTemplate.header("X-Internal-Identity", auth.getName());
                }
            }
        };
    }
}