package com.mustafa.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class BankUserAccessDeniedHandler implements AccessDeniedHandler {
    private final BankUserAccess bankUserAccess;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean restricted;
        try {
            restricted = bankUserAccess.isCurrentUserRestricted(authentication);
        } catch (RuntimeException ignored) {
            restricted = false;
        }
        if (restricted) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"code\":\"ACCOUNT_ACCESS_RESTRICTED\"}");
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
