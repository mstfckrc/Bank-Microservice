package com.mustafa.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPaymentSettingsRequest {

    private boolean autoPaymentEnabled;

    @Min(value = 1, message = "Ödeme günü 1'den küçük olamaz")
    @Max(value = 31, message = "Ödeme günü 31'den büyük olamaz")
    private Integer paymentDay;

    private String defaultSalaryIban;
}