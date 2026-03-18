package com.mustafa.controller.impl;

import com.mustafa.controller.IAuthController;
import com.mustafa.dto.request.LoginRequest;
import com.mustafa.dto.request.RegisterRequest;
import com.mustafa.dto.response.AuthResponse;
import com.mustafa.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j // 🚀 LOGGER AKTİF
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthControllerImpl implements IAuthController {

    private final IAuthService authService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        // Hangi rolle kayıt olmak istediğini logluyoruz
        log.info("REST İsteği: Yeni kullanıcı kaydı başlatılıyor. İstenen Rol: {}", request.getRole());
        return ResponseEntity.ok(authService.register(request));
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        log.info("REST İsteği: Kullanıcı giriş (Login) talebi alındı.");
        return ResponseEntity.ok(authService.login(request));
    }
}