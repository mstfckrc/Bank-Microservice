package com.mustafa.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySyncRequest {
    private String companyIdentityNumber; // Kurumsal servisteki isimle aynı!
    private String companyName;
    private String contactEmail;
    private String taxOffice;
}