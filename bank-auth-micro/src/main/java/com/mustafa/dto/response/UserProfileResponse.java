package com.mustafa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private String identityNumber;
    private String profileName; // Bireyse Ad-Soyad, Şirketse Ünvan dönecek
    private String email;
    private String role;
    private String status;
}