package com.mustafa.controller;

import com.mustafa.dto.request.InternalPaymentRequest;
import org.springframework.http.ResponseEntity;

public interface IInternalTransactionController {

    // 🚀 Sadece içerideki mikroservislerin tetikleyeceği para kesme emri
    ResponseEntity<Void> deductBillPayment(String identityNumber, InternalPaymentRequest request);

}