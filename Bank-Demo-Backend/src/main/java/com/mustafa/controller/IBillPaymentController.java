package com.mustafa.controller;

import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface IBillPaymentController {

    // Yeni Talimat Ekleme Kapısı
    ResponseEntity<BillInstructionResponse> createInstruction(Principal principal, @Valid @RequestBody BillInstructionRequest request);

    // Mevcut Talimatları Listeleme Kapısı
    ResponseEntity<List<BillInstructionResponse>> getMyInstructions(Principal principal);

    // Talimat İptal (Silme) Kapısı
    ResponseEntity<Map<String, String>> deleteInstruction(Principal principal, @PathVariable Long id);
}