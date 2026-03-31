package com.mustafa.client;

import com.mustafa.dto.request.CompanySyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "bank-corporate-micro", path = "/api/v1/internal/companies")
public interface CompanyServiceClient {

    // 1. Şirket Kayıt (AuthServiceImpl kullanıyor)
    @PostMapping("/sync")
    void syncNewCompany(@RequestBody CompanySyncRequest request);

    // 2. Şirket Güncelleme (AdminUserServiceImpl kullanacak)
    @PutMapping("/sync/{identityNumber}")
    void updateCompanyInfo(@PathVariable("identityNumber") String identityNumber, @RequestBody CompanySyncRequest request);

    // 3. Şirket Silme (AdminUserServiceImpl kullanacak)
    @DeleteMapping("/sync/{identityNumber}")
    void deleteCompanyInfo(@PathVariable("identityNumber") String identityNumber);
}