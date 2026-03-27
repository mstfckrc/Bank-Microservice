package com.mustafa.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Karargahtan (Feign'den) gelen hataları yakalar ve Karargahın asıl mesajını UI'a iletir
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeignStatusException(feign.FeignException e) {
        Map<String, String> response = new HashMap<>();

        // 🚀 BÜYÜK DEĞİŞİM: Eğer gövde boşsa bile HTTP Durum Kodunu (404, 403, 500) ve Feign'in kendi teknik mesajını ekrana basıyoruz!
        String realErrorMessage = "HTTP KODU: " + e.status() + " | TEKNİK DETAY: " + e.getMessage();

        if (e.contentUTF8() != null && !e.contentUTF8().isEmpty()) {
            realErrorMessage += " | KARARGAH MESAJI: " + e.contentUTF8();
        }

        response.put("error", "Karargah (Backend) İletişim Hatası");
        response.put("message", realErrorMessage);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Diğer genel hatalar için kalkan
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Sistem Hatası");
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}