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

    // 🚀 1. ÇÖZÜM: Jackson'ın "null" çökmesini engelleyen Boolean sarmalayıcı (Wrapper)
    private Boolean isSalaryPayment;

    // 🚀 2. ÇÖZÜM: Service katmanının (request.isSalaryPayment()) hata vermemesi için özel metod
    public boolean isSalaryPayment() {
        return Boolean.TRUE.equals(this.isSalaryPayment);
    }

    // 🚀 3. ÇÖZÜM: Derleme hatasını (cannot find symbol setSalaryPayment) çözen özel setter
    public void setSalaryPayment(Boolean salaryPayment) {
        this.isSalaryPayment = salaryPayment;
    }
}