package com.mustafa.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Kimlik/Vergi Numarası boş olamaz")
    @Size(min = 10, max = 11, message = "Kimlik/Vergi Numarası 10 veya 11 haneli olmalıdır")
    private String identityNumber; // TC (11) veya Vergi No (10)

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String password;

    @NotBlank(message = "Rol belirtilmelidir (RETAIL_CUSTOMER veya CORPORATE_MANAGER)")
    private String role; // Frontend buradan birey mi şirket mi olduğunu söyleyecek

    @Email(message = "Geçerli bir email adresi giriniz")
    @NotBlank(message = "Email boş olamaz")
    private String email;

    // --- BİREYSEL MÜŞTERİ İÇİN GEREKLİ ALANLAR ---
    private String firstName;
    private String lastName;

    // --- KURUMSAL ŞİRKET İÇİN GEREKLİ ALANLAR ---
    private String companyName;
    private String taxOffice;
}