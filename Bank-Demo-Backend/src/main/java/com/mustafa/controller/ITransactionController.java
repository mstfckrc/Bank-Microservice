package com.mustafa.controller;

import com.mustafa.dto.request.DepositRequest;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface ITransactionController {

    // Hesaba dışarıdan (ATM) para yatırma
    ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request);

    // Kendi hesabından başka bir hesaba para gönderme (Havale/EFT)
    ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request);

    // Müşterinin kendi hesap hareketlerini görmesi
    ResponseEntity<List<TransactionResponse>> getAccountTransactions(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    );
}