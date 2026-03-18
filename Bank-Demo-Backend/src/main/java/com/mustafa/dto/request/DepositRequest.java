package com.mustafa.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepositRequest {

    @NotBlank(message = "Paranın yatırılacağı IBAN boş bırakılamaz")
    private String iban;

    @NotNull(message = "Miktar boş bırakılamaz")
    @DecimalMin(value = "1.0", message = "En az 1.00 tutarında işlem yapılabilir") // Eksi veya 0 gönderilmesini engeller
    private BigDecimal amount;
}