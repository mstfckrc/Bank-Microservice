package com.mustafa.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillInstructionRequest {

    @NotNull(message = "Kasa seçimi zorunludur")
    private Long accountId;

    @NotBlank(message = "Fatura tipi zorunludur")
    private String billType; // ELECTRICITY, WATER, INTERNET, GAS

    @NotBlank(message = "Abone numarası zorunludur")
    private String subscriberNo;

    @NotNull(message = "Ödeme günü zorunludur")
    @Min(value = 1, message = "Gün en az 1 olabilir")
    @Max(value = 31, message = "Gün en fazla 31 olabilir")
    private Integer paymentDay;
}