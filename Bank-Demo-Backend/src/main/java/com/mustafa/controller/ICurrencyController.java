package com.mustafa.controller;

import com.mustafa.dto.response.ExchangeRateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface ICurrencyController {

    // Belirli bir para birimine (Örn: TRY) göre canlı kurları frontend'e sunar
    ResponseEntity<ExchangeRateResponse> getRates(
            @RequestParam(defaultValue = "TRY", required = false) String base
    );

}