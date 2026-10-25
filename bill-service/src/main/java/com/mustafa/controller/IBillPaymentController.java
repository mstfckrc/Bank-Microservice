package com.mustafa.controller;

import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public interface IBillPaymentController {

    ResponseEntity<BillInstructionResponse> createInstruction(Jwt principal, BillInstructionRequest request);

    ResponseEntity<List<BillInstructionResponse>> getMyInstructions(Jwt principal);

    ResponseEntity<Map<String, String>> deleteInstruction(Jwt principal, Long id);
}
