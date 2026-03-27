package com.mustafa.client;

import com.mustafa.dto.request.CompanySyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 🚀 HEDEF: Kurumsal Servis (Docker Compose'daki adı)
@FeignClient(name = "bank-corporate-micro", path = "/api/v1/internal/companies")
public interface CompanyServiceClient {

    @PostMapping("/sync")
    void syncNewCompany(@RequestBody CompanySyncRequest request);
}