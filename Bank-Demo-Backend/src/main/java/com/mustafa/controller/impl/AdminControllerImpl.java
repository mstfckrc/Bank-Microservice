package com.mustafa.controller.impl;

import com.mustafa.controller.IAdminController;
import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.service.IAdminService;
import com.mustafa.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j // 🚀 LOGGER AKTİF
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminControllerImpl implements IAdminController {

    private final IAdminService adminService;
    private final ITransactionService transactionService;

    @Override
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.info("REST İsteği: Admin tarafından sistemdeki tüm hesaplar sorgulanıyor.");
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @Override
    @GetMapping("/customers/{tcNo}/accounts")
    public ResponseEntity<List<AccountResponse>> getCustomerAccounts(@PathVariable String tcNo) {
        log.info("REST İsteği: Admin tarafından belirli bir müşterinin ({}) hesapları sorgulanıyor.", tcNo);
        return ResponseEntity.ok(adminService.getCustomerAccounts(tcNo));
    }

    @Override
    @GetMapping("/customers")
    public ResponseEntity<List<UserProfileResponse>> getAllCustomers() {
        log.info("REST İsteği: Admin tarafından sistemdeki tüm müşteriler sorgulanıyor.");
        return ResponseEntity.ok(adminService.getAllCustomers());
    }

    @Override
    @DeleteMapping("/customers/{tcNo}")
    public ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable String tcNo) {
        log.info("REST İsteği: Admin tarafından müşteri ({}) silme işlemi tetiklendi.", tcNo);
        adminService.deleteCustomer(tcNo);
        Map<String, String> response = new HashMap<>();
        response.put("message", tcNo + " kimlik numaralı müşteri ve bağlı tüm hesapları başarıyla silinmiştir.");
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/customers/{tcNo}")
    public ResponseEntity<UserProfileResponse> updateCustomer(@PathVariable String tcNo, @RequestBody UpdateProfileRequest request) {
        log.info("REST İsteği: Admin tarafından müşteri ({}) profil güncelleme işlemi tetiklendi.", tcNo);
        return ResponseEntity.ok(adminService.updateCustomer(tcNo, request));
    }

    @Override
    @GetMapping("/accounts/{accountNumber}/transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(@PathVariable String accountNumber) {
        log.info("REST İsteği: Admin tarafından bir hesabın ({}) işlem geçmişi sorgulanıyor.", accountNumber);
        return ResponseEntity.ok(adminService.getAccountTransactions(accountNumber));
    }

    @Override
    @PostMapping("/customers/{tcNo}/accounts")
    public ResponseEntity<AccountResponse> openAccountForCustomer(
            @PathVariable String tcNo,
            @RequestBody OpenAccountRequest request) {
        log.info("REST İsteği: Admin tarafından müşteriye ({}) yeni hesap açma işlemi başlatıldı. Döviz: {}", tcNo, request.getCurrency());
        return ResponseEntity.ok(adminService.openAccountForCustomer(tcNo, request));
    }

    @PutMapping("/customers/{tcNo}/status")
    @Override
    public ResponseEntity<Map<String, String>> updateCustomerStatus(
            @PathVariable String tcNo,
            @RequestParam String status) {
        log.info("REST İsteği: Admin tarafından müşteri ({}) onay durumu değiştiriliyor. Yeni Durum: {}", tcNo, status);
        adminService.updateCustomerStatus(tcNo, status);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Müşteri durumu başarıyla güncellendi: " + status);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String status) {
        log.info("REST İsteği: Admin tarafından tüm transferler sorgulanıyor. Durum Filtresi: {}", status != null ? status : "TÜMÜ");
        return ResponseEntity.ok(transactionService.getAllTransactionsForAdmin(status));
    }

    @Override
    @PutMapping("/transactions/{referenceNo}/approve")
    public ResponseEntity<TransactionResponse> approveTransaction(@PathVariable String referenceNo) {
        log.info("REST İsteği: Admin tarafından MASAK limitli işleme ({}) ONAY veriliyor.", referenceNo);
        return ResponseEntity.ok(transactionService.approveTransaction(referenceNo));
    }

    @Override
    @PutMapping("/transactions/{referenceNo}/reject")
    public ResponseEntity<TransactionResponse> rejectTransaction(@PathVariable String referenceNo) {
        log.info("REST İsteği: Admin tarafından MASAK limitli işlem ({}) REDDEDİLİYOR.", referenceNo);
        return ResponseEntity.ok(transactionService.rejectTransaction(referenceNo));
    }
}