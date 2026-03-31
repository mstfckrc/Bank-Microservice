package com.mustafa.controller;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.TransactionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 🚀 Bütün iç kapılar /api/v1/internal yolundan açılır!
@RequestMapping("/api/v1/internal")
public interface IInternalBankController {

    @GetMapping("/accounts/validate")
    ResponseEntity<AccountValidationResponse> validateAccount(@RequestParam("iban") String iban);

    @PostMapping("/transactions/bulk-salary")
    ResponseEntity<List<TransactionResponse>> payBulkSalaries(@RequestBody BulkSalaryRequest request);
}