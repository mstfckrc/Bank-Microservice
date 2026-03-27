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
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    // 🚀 Gateway'in içeriye fırlattığı başlık adı (Eğer Gateway farklı bir isim atıyorsa burayı ona göre değiştir)
    private static final String IDENTITY_HEADER = "X-Identity-Number";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Kapıdaki Gateway'den gelen yaka kartını okuyoruz
        String identityNumber = request.getHeader(IDENTITY_HEADER);

        if (identityNumber != null && !identityNumber.isEmpty()) {
            // Spring Security'nin beklediği o "Principal" nesnesini sadece TC numarasıyla (yetki kontrolsüz) dolduruyoruz
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(identityNumber, null, Collections.emptyList());

            // Zırhı kuşandır! Artık Controller'daki principal.getName() patlamayacak!
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // İsteğin yoluna devam etmesine izin ver
        filterChain.doFilter(request, response);
    }
}