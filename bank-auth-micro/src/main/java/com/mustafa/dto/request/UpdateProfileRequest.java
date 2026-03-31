package com.mustafa.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    // Profil güncellenirken sadece bu iki alanın değişmesine izin veriyoruz
    private String email;
    private String profileName; // Bireyler için Ad Soyad, Şirketler için Şirket Adı
}