package com.mustafa.controller.impl;

import com.mustafa.controller.IBillPaymentController;
import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import com.mustafa.service.IBillPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/bills/instructions")
@RequiredArgsConstructor
public class BillPaymentControllerImpl implements IBillPaymentController {

    private final IBillPaymentService billPaymentService;

    @Override
    @PostMapping
    public ResponseEntity<BillInstructionResponse> createInstruction(
            Principal principal,
            @Valid @RequestBody BillInstructionRequest request) {

        log.info("REST İsteği: Yeni fatura otomatik ödeme talimatı oluşturma");
        return new ResponseEntity<>(billPaymentService.createInstruction(principal.getName(), request), HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<BillInstructionResponse>> getMyInstructions(Principal principal) {

        log.info("REST İsteği: Kullanıcının fatura talimatları listeleniyor");
        return ResponseEntity.ok(billPaymentService.getMyInstructions(principal.getName()));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteInstruction(
            Principal principal,
            @PathVariable Long id) {

        log.info("REST İsteği: Fatura talimatı silme (Talimat ID: {})", id);
        billPaymentService.deleteInstruction(principal.getName(), id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Fatura otomatik ödeme talimatı başarıyla iptal edildi.");
        return ResponseEntity.ok(response);
    }
}