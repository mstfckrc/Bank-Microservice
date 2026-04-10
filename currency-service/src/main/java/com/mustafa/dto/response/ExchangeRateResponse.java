package com.mustafa.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class ExchangeRateResponse implements Serializable {
    // 🚀 1. ZIRH: Redis'in bu sınıfı Byte'a çevirebilmesi için gereken zorunlu kimlik.
    private static final long serialVersionUID = 1L;

    private String base_code;
    private Map<String, Double> rates;
}