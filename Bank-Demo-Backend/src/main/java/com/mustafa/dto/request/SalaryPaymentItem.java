package com.mustafa.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPaymentItem {
    private String receiverIban; // Personelin kasası
    private BigDecimal amount;   // Alacağı maaş (TRY)
}