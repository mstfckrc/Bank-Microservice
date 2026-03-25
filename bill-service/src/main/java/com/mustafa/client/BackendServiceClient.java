package com.mustafa.client;

import com.mustafa.dto.request.InternalPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// 🚀 Karargahın (Backend) Eureka'daki tam adını ve açacağımız gizli kapının yolunu yazıyoruz.
@FeignClient(name = "bank-demo-backend", path = "/api/v1/internal/transactions")
public interface BackendServiceClient {

    @PostMapping("/bill-payment")
    void deductBillPayment(
            @RequestHeader("X-Identity-Number") String identityNumber, // Kapıdan geçen kimlik
            @RequestBody InternalPaymentRequest request // Para kesme emri
    );
}