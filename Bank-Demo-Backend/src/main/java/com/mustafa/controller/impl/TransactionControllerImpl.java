package com.mustafa.controller.impl;

import com.mustafa.controller.ITransactionController;
import com.mustafa.dto.request.DepositRequest;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j // 🚀 LOGGER AKTİF
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionControllerImpl implements ITransactionController {

    private final ITransactionService transactionService;

    @Override
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@RequestBody DepositRequest request) {
        log.info("REST İsteği: Para yatırma (Deposit) talebi alındı. Hedef IBAN: {}", request.getIban());
        return ResponseEntity.ok(transactionService.deposit(request));
    }

    @Override
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransferRequest request) {
        log.info("REST İsteği: Para transferi (Transfer) talebi alındı.");
        return ResponseEntity.ok(transactionService.transfer(request));
    }

    @Override
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("REST İsteği: Hesap hareketleri (Ekstre) sorgulanıyor. Hesap No: {}", accountNumber);
        return ResponseEntity.ok(transactionService.getAccountTransactions(accountNumber, type, startDate, endDate));
    }
}