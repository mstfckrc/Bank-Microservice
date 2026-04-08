package com.mustafa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InternalHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 🚀 BÜYÜK DEĞİŞİM: Artık '/internal/' kısıtlaması YOK!
        // Çünkü Süper Gateway tüm istekler için (müşteri veya iç hat) bu Header'ları yolluyor.
        String identityNumber = request.getHeader("X-Identity-Number");
        String rolesHeader = request.getHeader("X-User-Role");

        if (identityNumber != null && !identityNumber.isEmpty()) {

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // Gateway'in yolladığı Rolleri (Örn: ROLE_ADMIN, ROLE_CUSTOMER) Karargahın diline çeviriyoruz
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(role -> {
                            // Gateway "ROLE_" eklemişse tekrar ekleme, eklememişse ekle
                            String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                            return new SimpleGrantedAuthority(finalRole);
                        })
                        .collect(Collectors.toList());
            }

            // Adamın TC'sini ve Rollerini kuşanıp kapıdan içeri alıyoruz!
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(identityNumber, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}