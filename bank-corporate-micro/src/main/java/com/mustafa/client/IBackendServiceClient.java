package com.mustafa.client;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// 🚀 Karargahı (bank-demo-backend) ismiyle bul ve gizli yeraltı kapısına (/api/v1/internal) bağlan!
@FeignClient(name = "bank-demo-backend", path = "/api/v1/internal")
public interface IBackendServiceClient {

    // 1. Kasa (IBAN) Sahibini ve Durumunu Doğrula
    @GetMapping("/accounts/validate")
    AccountValidationResponse validateAccount(@RequestParam("iban") String iban);

    // 2. Karargaha Devasa Maaş Listesini Fırlat ve Dekontları (Transaction) Geri Al!
    @PostMapping("/transactions/bulk-salary")
    List<TransactionResponse> payBulkSalaries(@RequestBody BulkSalaryRequest request);
}