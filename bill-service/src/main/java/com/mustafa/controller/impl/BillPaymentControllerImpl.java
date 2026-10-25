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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/bills/instructions")
@RequiredArgsConstructor
public class BillPaymentControllerImpl implements IBillPaymentController {

    private final IBillPaymentService billPaymentService;

    // The identity is taken from the JWT already verified by this service, not from a caller-supplied header.
    private void validateIdentityNumber(String identityNumber) {
        if (identityNumber == null || !identityNumber.matches("^[0-9]{11}$")) {
            throw new IllegalArgumentException("Geçersiz Kimlik Numarası! TC/Vergi No tam 11 haneli rakamlardan oluşmalıdır.");
        }
    }

    private String identityNumber(Jwt principal) {
        String identityNumber = principal.getClaimAsString("bank_identity_number");
        validateIdentityNumber(identityNumber);
        return identityNumber;
    }

    @Override
    @PostMapping
    public ResponseEntity<BillInstructionResponse> createInstruction(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody BillInstructionRequest request) {
        String identityNumber = identityNumber(principal);

        log.info("REST İsteği: Yeni fatura otomatik ödeme talimatı oluşturma.");
        return new ResponseEntity<>(billPaymentService.createInstruction(identityNumber, request), HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<BillInstructionResponse>> getMyInstructions(
            @AuthenticationPrincipal Jwt principal) {
        String identityNumber = identityNumber(principal);

        log.info("REST İsteği: Kullanıcının fatura talimatları listeleniyor.");
        return ResponseEntity.ok(billPaymentService.getMyInstructions(identityNumber));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteInstruction(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable Long id) {
        String identityNumber = identityNumber(principal);

        log.info("REST İsteği: Fatura talimatı silme (Talimat ID: {}).", id);
        billPaymentService.deleteInstruction(identityNumber, id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Fatura otomatik ödeme talimatı başarıyla iptal edildi.");
        return ResponseEntity.ok(response);
    }
}
