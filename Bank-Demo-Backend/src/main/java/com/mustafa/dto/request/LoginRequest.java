package com.mustafa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Kimlik/Vergi Numarası boş olamaz")
    @Size(min = 10, max = 11, message = "Kimlik/Vergi Numarası 10 veya 11 haneli olmalıdır")
    private String identityNumber;

    @NotBlank(message = "Şifre boş olamaz")
    private String password;
}