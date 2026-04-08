package com.mustafa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 🚀 BÜYÜK DEVRİM: UserDetailsService TAMAMEN SİLİNDİ!
    // Karargah artık yetki için Veritabanına (AppUser'a) ASLA gitmeyecek!

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Sınır Kapısından (Gateway) gelen zımbalanmış kimlik ve ROLÜ oku!
        final String userTcNo = request.getHeader("X-User-TC");
        final String userRole = request.getHeader("X-User-Role"); // 🚀 YENİ EKLENDİ

        // 2. Gateway TC veya Rolü eklememişse (Örn: /register gibi açık bir kapıysa) devam et.
        if (userTcNo == null || userRole == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. TC ve Rol var, sistemde henüz oturum açılmamışsa
        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            // 🚀 Gateway bu adamı içeri soktuysa %100 temizdir.
            // Rolü doğrudan String'den Spring Security formatına (GrantedAuthority) çeviriyoruz:
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(userRole));

            // Kimlik Kartını (Token) doğrudan Gateway'in verdiği bilgilerle basıyoruz!
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userTcNo, // Principal olarak sadece TC'yi tutuyoruz (UserDetails nesnesine gerek kalmadı)
                    null,     // Şifreye gerek yok
                    authorities // Rolleri verdik (Örn: ROLE_ADMIN, ROLE_RETAIL_CUSTOMER)
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Komutanı sisteme yetkili olarak kaydet
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // İsteği yoluna devam ettir
        filterChain.doFilter(request, response);
    }
}