package com.mustafa.controller;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.TransactionResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IInternalBankController {

    ResponseEntity<AccountValidationResponse> validateAccount(String iban);

    ResponseEntity<List<TransactionResponse>> payBulkSalaries(BulkSalaryRequest request);

    // 🚀 YENİ EKLENDİ: Müşteri hesaplarını silme telsiz hattı
    ResponseEntity<Void> deleteCustomerAccounts(String identityNumber);
}