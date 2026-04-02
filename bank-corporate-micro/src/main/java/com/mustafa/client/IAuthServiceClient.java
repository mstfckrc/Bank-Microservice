package com.mustafa.client;

import com.mustafa.dto.response.CustomerProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 🚀 Nüfus Müdürlüğüne (auth-service) bağlanan telsiz
@FeignClient(name = "auth-service", path = "/api/v1/internal/users")
public interface IAuthServiceClient {

    // TC Kimlik No ile Bireysel Müşteri Profilini Getir
    @GetMapping("/profile/{identityNumber}")
    CustomerProfileResponse getCustomerProfile(@PathVariable("identityNumber") String identityNumber);
}