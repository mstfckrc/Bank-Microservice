package com.mustafa.controller.impl;

import com.mustafa.controller.ICurrencyController;
import com.mustafa.dto.response.ExchangeRateResponse;
import com.mustafa.service.ICurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j // 🚀 LOGGER AKTİF
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
public class CurrencyControllerImpl implements ICurrencyController {

    private final ICurrencyService currencyService;

    @Override
    @GetMapping("/rates")
    public ResponseEntity<ExchangeRateResponse> getRates(String base) {
        log.info("REST İsteği: Canlı kur bilgisi sorgulanıyor. Temel Döviz (Base): {}", base);
        return ResponseEntity.ok(currencyService.getLiveRates(base));
    }
}