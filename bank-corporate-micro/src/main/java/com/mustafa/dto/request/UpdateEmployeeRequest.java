package com.mustafa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {

    @NotBlank(message = "Yeni IBAN boş olamaz")
    private String salaryIban;

    @NotNull(message = "Yeni maaş miktarı boş olamaz")
    @Positive(message = "Maaş sıfırdan büyük olmalıdır")
    private BigDecimal salaryAmount;
}