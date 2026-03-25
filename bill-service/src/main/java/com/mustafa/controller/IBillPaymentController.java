package com.mustafa.controller;

import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public interface IBillPaymentController {

    // 🚀 MİKROSERVİS DEVRİMİ: Principal yerine Header'dan kimlik okuyoruz!
    ResponseEntity<BillInstructionResponse> createInstruction(String identityNumber, BillInstructionRequest request);

    ResponseEntity<List<BillInstructionResponse>> getMyInstructions(String identityNumber);

    ResponseEntity<Map<String, String>> deleteInstruction(String identityNumber, Long id);
}