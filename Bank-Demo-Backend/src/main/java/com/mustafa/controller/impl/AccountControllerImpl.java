package com.mustafa.controller.impl;

import com.mustafa.controller.IAccountController;
import com.mustafa.dto.request.CreateAccountRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.service.IAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j // 🚀 LOGGER AKTİF
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountControllerImpl implements IAccountController {

    private final IAccountService accountService;

    @Override
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(CreateAccountRequest request) {
        log.info("REST İsteği: Yeni hesap açma talebi alındı. İstenen Döviz Cinsi: {}", request.getCurrency());
        return new ResponseEntity<>(accountService.createAccount(request), HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        log.info("REST İsteği: Kullanıcının hesap listesi sorgulanıyor.");
        return ResponseEntity.ok(accountService.getMyAccounts());
    }

    @Override
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber) {
        log.info("REST İsteği: Hesap detayları sorgulanıyor. Hesap No: {}", accountNumber);
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @Override
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<String> deleteAccount(@PathVariable String accountNumber) {
        log.info("REST İsteği: Hesap kapatma talebi alındı. Hesap No: {}", accountNumber);
        accountService.deleteAccount(accountNumber);
        return ResponseEntity.ok("Hesap başarıyla kapatıldı.");
    }
}