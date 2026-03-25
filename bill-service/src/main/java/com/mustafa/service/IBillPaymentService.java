package com.mustafa.service;

import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;

import java.util.List;

public interface IBillPaymentService {
    // 🤖 Sistem/Zamanlayıcı için (Artık telsizle Karargaha emir veriyor)
    void payBillAutomatically(Long instructionId, String identityNumber);

    // 👤 Normal Kullanıcı (Frontend) için
    BillInstructionResponse createInstruction(String identityNumber, BillInstructionRequest request);
    List<BillInstructionResponse> getMyInstructions(String identityNumber);
    void deleteInstruction(String identityNumber, Long instructionId);
}