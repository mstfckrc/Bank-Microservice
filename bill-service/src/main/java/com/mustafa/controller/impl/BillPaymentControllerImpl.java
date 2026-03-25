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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/bills/instructions")
@RequiredArgsConstructor
public class BillPaymentControllerImpl implements IBillPaymentController {

    private final IBillPaymentService billPaymentService;

    // 🛡️ GÜVENLİK KALKANI: Gateway aşılsa bile içeride TC doğrulaması yapıyoruz!
    private void validateIdentityNumber(String identityNumber) {
        if (identityNumber == null || !identityNumber.matches("^[0-9]{11}$")) {
            throw new IllegalArgumentException("Geçersiz Kimlik Numarası! TC/Vergi No tam 11 haneli rakamlardan oluşmalıdır.");
        }
    }

    @Override
    @PostMapping
    public ResponseEntity<BillInstructionResponse> createInstruction(
            @RequestHeader("X-Identity-Number") String identityNumber, // 🛡️ Gateway'den gelen gizli not
            @Valid @RequestBody BillInstructionRequest request) {

        // 🚀 KAPI KONTROLÜ: İstek içeri girmeden önce TC kontrol ediliyor
        validateIdentityNumber(identityNumber);

        log.info("REST İsteği: Yeni fatura otomatik ödeme talimatı oluşturma. İstek Sahibi: {}", identityNumber);
        return new ResponseEntity<>(billPaymentService.createInstruction(identityNumber, request), HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<BillInstructionResponse>> getMyInstructions(
            @RequestHeader("X-Identity-Number") String identityNumber) {

        // 🚀 KAPI KONTROLÜ
        validateIdentityNumber(identityNumber);

        log.info("REST İsteği: Kullanıcının fatura talimatları listeleniyor. İstek Sahibi: {}", identityNumber);
        return ResponseEntity.ok(billPaymentService.getMyInstructions(identityNumber));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteInstruction(
            @RequestHeader("X-Identity-Number") String identityNumber,
            @PathVariable Long id) {

        // 🚀 KAPI KONTROLÜ
        validateIdentityNumber(identityNumber);

        log.info("REST İsteği: Fatura talimatı silme (Talimat ID: {}, İstek Sahibi: {})", id, identityNumber);
        billPaymentService.deleteInstruction(identityNumber, id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Fatura otomatik ödeme talimatı başarıyla iptal edildi.");
        return ResponseEntity.ok(response);
    }
}