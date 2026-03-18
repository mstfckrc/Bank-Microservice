package com.mustafa.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class ExchangeRateResponse {
    private String base_code; // Örn: TRY
    private Map<String, Double> rates; // Kurlar: USD: 0.03, EUR: 0.028 vb.
}