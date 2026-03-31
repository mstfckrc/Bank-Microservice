package com.mustafa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class InternalHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 🚀 SADECE iç hattan (diğer mikroservislerden) gelen isteklerde devreye gir!
        if (request.getRequestURI().startsWith("/api/v1/internal/")) {
            String identityNumber = request.getHeader("X-Identity-Number");

            if (identityNumber != null && !identityNumber.isEmpty()) {
                // Feign'den gelen bu TC'yi alıp Karargahın hafızasına güvenli bir şekilde oturtuyoruz
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(identityNumber, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}