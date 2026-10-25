package com.mustafa.client;

import com.mustafa.dto.request.InternalPaymentRequest;
import com.mustafa.client.config.CoreServiceClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 🚀 Karargahın (Backend) Eureka'daki tam adını ve açacağımız gizli kapının yolunu yazıyoruz.
@FeignClient(name = "bank-demo-backend", path = "/api/v1/internal/transactions", configuration = CoreServiceClientConfig.class)
public interface BackendServiceClient {

    @PostMapping("/bill-payment")
    void deductBillPayment(@RequestBody InternalPaymentRequest request);
}
