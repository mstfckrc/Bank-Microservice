package com.mustafa.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPaymentRequest {

    @NotBlank(message = "Maaşların dağıtılacağı kasa IBAN'ı boş olamaz")
    private String senderIban;
}