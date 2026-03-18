package com.mustafa.service;

import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import com.mustafa.dto.response.TransactionResponse;

import java.util.List;

public interface IBillPaymentService {
    // 🤖 Sistem/Zamanlayıcı için (Artık void değil, işlemin sonucunu dönüyor)
    TransactionResponse payBillAutomatically(Long instructionId);

    // 👤 Normal Kullanıcı (Frontend) için
    BillInstructionResponse createInstruction(String identityNumber, BillInstructionRequest request);
    List<BillInstructionResponse> getMyInstructions(String identityNumber);
    void deleteInstruction(String identityNumber, Long instructionId);
}