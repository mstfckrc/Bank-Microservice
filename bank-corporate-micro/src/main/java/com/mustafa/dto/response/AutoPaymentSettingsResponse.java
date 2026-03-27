package com.mustafa.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AutoPaymentSettingsResponse {
    private boolean autoPaymentEnabled;
    private Integer paymentDay;
    private String defaultSalaryIban;
    private String message; // Frontend'de Toast olarak basacağımız o şık mesaj
}