package com.mustafa.dto.request;

import lombok.Data;

@Data
public class SalaryPaymentRequest {
    private String senderIban; // Şirketin maaşları dağıtacağı kasanın IBAN'ı
}