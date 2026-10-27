package com.mustafa.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class UpdateProfileRequest {
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir.")
    private String email;
    @Size(max = 150, message = "Şirket unvanı en fazla 150 karakter olabilir.")
    private String profileName;
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
    private String firstName;
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
    private String lastName;
}
