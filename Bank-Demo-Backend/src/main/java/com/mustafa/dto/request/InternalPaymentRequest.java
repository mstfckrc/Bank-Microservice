package com.mustafa.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InternalPaymentRequest {
    private Long accountId;
    private BigDecimal amount;
    private String description;
}