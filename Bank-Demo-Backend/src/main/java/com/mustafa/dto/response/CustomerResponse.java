package com.mustafa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private String tcNo;
    private String fullName;
    private String email;
    private String role; // YENİ EKLENEN ALAN
    private String status; // 🚀 YENİ EKLENEN ALAN (PENDING, APPROVED vs. dönecek)
}