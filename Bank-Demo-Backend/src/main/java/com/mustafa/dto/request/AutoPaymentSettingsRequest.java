package com.mustafa.dto.request;

import lombok.Data;

@Data
public class AutoPaymentSettingsRequest {
    private boolean autoPaymentEnabled;
    private Integer paymentDay; // 1-31 arası
    private String defaultSalaryIban;
}