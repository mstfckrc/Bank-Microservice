package com.mustafa.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignIdentityInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Kurumsal servis "Ben kimim?" diye kendi içine bakar
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
            // Karargaha atılan her telsizin (Feign) üstüne TC'yi zımbalar!
            template.header("X-Identity-Number", auth.getName());
        }
    }
}