package com.mustafa.controller;

import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.SystemLogResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.dto.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface IAdminController {
    ResponseEntity<List<AccountResponse>> getAllAccounts();
    ResponseEntity<List<AccountResponse>> getCustomerAccounts(@PathVariable String tcNo);
    ResponseEntity<List<TransactionResponse>> getAccountTransactions(String accountNumber);
    ResponseEntity<AccountResponse> openAccountForCustomer(String tcNo, OpenAccountRequest request);

    // --- 🚀 MERKEZİ İŞLEM İZLEME VE ONAY (GOD MODE) ---
    ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String status);

    ResponseEntity<TransactionResponse> approveTransaction(@PathVariable String referenceNo);

    ResponseEntity<TransactionResponse> rejectTransaction(@PathVariable String referenceNo);

    // --- 🚀 YENİ: İSTİHBARAT (LOG) İZLEME ---
    ResponseEntity<List<SystemLogResponse>> getSystemLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String level);
}