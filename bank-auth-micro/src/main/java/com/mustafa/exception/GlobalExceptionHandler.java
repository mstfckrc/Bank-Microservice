package com.mustafa.exception;

import com.mustafa.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // Tüm controller'ları dinleyen nöbetçi
public class GlobalExceptionHandler {

    // 1. Bizim fırlattığımız "Müşteri zaten var" hatası
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExistsException(CustomerAlreadyExistsException ex, WebRequest request) {
        ErrorResponse errorDetails = new ErrorResponse(LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT); // 409 Conflict döneriz
    }

    // 2. Spring Security'nin "Şifre Yanlış" hatası
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        ErrorResponse errorDetails = new ErrorResponse(LocalDateTime.now(), "TC Kimlik No veya Şifre hatalı!", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED); // 401 Unauthorized döneriz
    }

    // 3. DTO'lardaki @NotBlank, @Size (Validation) hataları
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        // Hangi alanda (Örn: tcNo) ne hatası (Örn: 11 haneli olmalı) var, tek tek ayıkla
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); // 400 Bad Request
    }

    @ExceptionHandler(BankOperationException.class)
    public ResponseEntity<ErrorResponse> handleBankOperationException(BankOperationException ex, WebRequest request) {
        ErrorResponse errorDetails = new ErrorResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST); // 400 Bad Request döneriz
    }

    // 4. Veritabanı Benzersizlik (Unique Constraint) Hataları (Örn: Aynı email)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        // Hata mesajını yakalayıp daha kullanıcı dostu bir hale getiriyoruz
        String message = "Bu bilgi (muhtemelen e-posta veya TC) sistemde zaten kayıtlı!";

        ErrorResponse errorDetails = new ErrorResponse(
                LocalDateTime.now(),
                message,
                request.getDescription(false)
        );

        // 403 yerine 400 Bad Request dönüyoruz ki frontend bizi sistemden atmasın!
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
}