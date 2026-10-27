package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalBankController;
import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.service.IInternalBankService;
import com.mustafa.service.application.PayrollApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal") // 🚀 Sınıf seviyesine taşındı
@RequiredArgsConstructor
public class InternalBankControllerImpl implements IInternalBankController {

    private final IInternalBankService internalBankService;
    private final PayrollApplicationService payrollApplicationService;

    @Override
    @GetMapping("/accounts/validate") // 🚀 Metot seviyesine taşındı
    public ResponseEntity<AccountValidationResponse> validateAccount(@RequestParam("iban") String iban) {
        log.info("INTERNAL REST İsteği: Hesap doğrulama talebi alındı.");
        return ResponseEntity.ok(internalBankService.validateAccount(iban));
    }

    @Override
    @PostMapping("/transactions/bulk-salary") // 🚀 Metot seviyesine taşındı
    public ResponseEntity<List<TransactionResponse>> payBulkSalaries(@RequestBody BulkSalaryRequest bulkRequest) {
        log.info("INTERNAL REST İsteği: Toplu maaş dağıtım emri alındı.");
        return ResponseEntity.ok(payrollApplicationService.payBulkSalaries(bulkRequest));
    }

    @Override
    @DeleteMapping("/accounts/customer/{identityNumber}") // 🚀 Metot seviyesine taşındı
    public ResponseEntity<Void> deleteCustomerAccounts(@PathVariable("identityNumber") String identityNumber) {
        log.info("INTERNAL REST İsteği: müşteri hesap kapatma emri alındı.");
        internalBankService.deleteCustomerAccounts(identityNumber);
        return ResponseEntity.ok().build();
    }
}
