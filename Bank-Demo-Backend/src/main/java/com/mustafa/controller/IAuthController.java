package com.mustafa.controller;

import com.mustafa.dto.request.LoginRequest;
import com.mustafa.dto.request.RegisterRequest;
import com.mustafa.dto.response.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

public interface IAuthController {

    // @Valid koyduk ki kapıda DTO kurallarımız (11 hane vs) çalışsın
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request);

    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);
}