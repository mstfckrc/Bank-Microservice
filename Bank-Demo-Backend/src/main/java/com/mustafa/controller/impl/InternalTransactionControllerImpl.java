package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalTransactionController;
import com.mustafa.dto.request.InternalPaymentRequest;
import com.mustafa.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/transactions")
@RequiredArgsConstructor
public class InternalTransactionControllerImpl implements IInternalTransactionController {

    private final ITransactionService transactionService;

    @Override
    @PostMapping("/bill-payment")
    public ResponseEntity<Void> deductBillPayment(@RequestBody InternalPaymentRequest request) {

        log.info("Fatura servisinden otomatik ödeme emri alındı.");

        transactionService.processInternalPayment(request);

        return ResponseEntity.ok().build();
    }
}
