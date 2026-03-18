package com.mustafa.controller;

import com.mustafa.dto.request.CreateAccountRequest;
import com.mustafa.dto.response.AccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface IAccountController {

    // Yeni hesap açma isteğini karşılar
    ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request);

    // Müşterinin kendi hesaplarını listelemesini sağlar
    ResponseEntity<List<AccountResponse>> getMyAccounts();

    // URL'den hesap numarasını alıp tekil hesap döner
    ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber);

    // Hesap kapatma isteğini karşılar
    ResponseEntity<String> deleteAccount(@PathVariable String accountNumber);
}