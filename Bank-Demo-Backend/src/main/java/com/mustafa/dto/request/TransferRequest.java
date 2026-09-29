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
public class TransferRequest {

    @NotBlank(message = "Gönderen IBAN boş bırakılamaz")
    private String senderIban;

    @NotBlank(message = "Alıcı IBAN boş bırakılamaz")
    private String receiverIban;

    @NotNull(message = "Transfer miktarı boş bırakılamaz")
    @DecimalMin(value = "1.0", message = "En az 1.00 tutarında transfer yapılabilir")
    private BigDecimal amount;

    // Opsiyonel açıklama alanı (Kira, Borç vb.)
    private String description;

}
