package com.mustafa.client;

import com.mustafa.dto.response.ExchangeRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 🚀 DÜZELTME: Hedefin YML dosyasındaki gerçek kimliğiyle (küçük harf) birebir aynı!
@FeignClient(name = "bank-currency-micro", path = "/api/v1/currencies")
public interface CurrencyServiceClient {

    @GetMapping("/rates")
    ExchangeRateResponse getRates(@RequestParam("base") String base);
}