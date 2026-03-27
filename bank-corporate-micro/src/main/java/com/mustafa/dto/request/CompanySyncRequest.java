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
    // 🚀 DÜZELTİLDİ: Entity'deki isimle birebir aynı yapıldı!
    private String companyIdentityNumber;
    private String companyName;
    private String contactEmail;
    private String taxOffice;
}