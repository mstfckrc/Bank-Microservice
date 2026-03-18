package com.mustafa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 🚀 DİKKAT: JwtService'i tamamen SİLİP ATTIK! Sınır Kapısı o işi halletti.
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Sınır Kapısından (Gateway) gelen zımbalanmış kimliği oku!
        final String userTcNo = request.getHeader("X-User-TC");

        // 2. Eğer Gateway TC'yi eklememişse (Örn: /register gibi açık bir kapıysa) devam et.
        if (userTcNo == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. TC var ve sistemde henüz oturum açılmamışsa
        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            // Mimarinin geri kalanını bozmamak için veritabanından kullanıcıyı bul
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userTcNo);

            // 🚀 ŞİFRE ÇÖZME VEYA DOĞRULAMA YOK!
            // Gateway bu adamı içeri soktuysa %100 temizdir. Doğrudan yetkiyi veriyoruz:
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Komutanı (Kullanıcıyı) sisteme yetkili olarak kaydet
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // İsteği yoluna devam ettir
        filterChain.doFilter(request, response);
    }
}