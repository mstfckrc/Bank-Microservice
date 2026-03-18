package com.mustafa.controller;

import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.dto.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

public interface IAdminController {
    ResponseEntity<List<UserProfileResponse>> getAllCustomers();
    ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable String tcNo);
    ResponseEntity<UserProfileResponse> updateCustomer(@PathVariable String tcNo, @RequestBody UpdateProfileRequest request);

    ResponseEntity<List<AccountResponse>> getAllAccounts();
    ResponseEntity<List<AccountResponse>> getCustomerAccounts(@PathVariable String tcNo);
    ResponseEntity<List<TransactionResponse>> getAccountTransactions(String accountNumber);
    ResponseEntity<AccountResponse> openAccountForCustomer(String tcNo, OpenAccountRequest request);

    ResponseEntity<Map<String, String>> updateCustomerStatus(
            @PathVariable String tcNo,
            @RequestParam String status);

    // --- 🚀 YENİ: MERKEZİ İŞLEM İZLEME VE ONAY (GOD MODE) ---

    // Tüm banka trafiğini (işlemleri) getir
    ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String status);

    // Yüklü işlemi onayla (Parayı alıcıya geçir)
    ResponseEntity<TransactionResponse> approveTransaction(@PathVariable String referenceNo);

    // Yüklü işlemi reddet (Parayı gönderene iade et)
    ResponseEntity<TransactionResponse> rejectTransaction(@PathVariable String referenceNo);
}