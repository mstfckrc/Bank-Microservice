package com.mustafa.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 🚀 Karargahın adresi ve iç hat yolu
@FeignClient(name = "bank-demo-backend", path = "/api/v1/internal")
public interface BackendServiceClient {

    // 🚀 İçinde para varsa Backend'in hata fırlatacağı o kilitli kapı
    @DeleteMapping("/accounts/customer/{identityNumber}")
    void deleteCustomerAccounts(@PathVariable("identityNumber") String identityNumber);
}