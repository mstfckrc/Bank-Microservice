package com.mustafa.dto.request;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class InternalPaymentRequest {
    private Long accountId; // Hangi kasadan?
    private BigDecimal amount; // Ne kadar kesilecek?
    private String description; // Dekont açıklaması ne olacak?
}