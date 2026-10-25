package com.mustafa.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSalaryRequest {
    private String senderIban;  // Şirketin çıkış kasası
    private String companyName; // Dekont açıklaması için şirket adı
    private String companyIdentityNumber; // İş bağlamı; Core gönderici kasa sahipliğini doğrular.
    private List<SalaryPaymentItem> salaryItems; // Personel maaş listesi
}
