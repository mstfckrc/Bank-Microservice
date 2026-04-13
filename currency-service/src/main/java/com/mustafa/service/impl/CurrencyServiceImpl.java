package com.mustafa.service.impl;

import com.mustafa.dto.response.ExchangeRateResponse;
import com.mustafa.exception.BankOperationException;
import com.mustafa.service.ICurrencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class CurrencyServiceImpl implements ICurrencyService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 🗑️ @Lazy self değişkenini ve constructor'ı tamamen SİLDİK!
    // Artık sınıflar birbirini kandırmayacak.

    // ========================================================================
    // 1. VİTRİN OPERASYONU (ÖNBELLEKLİ - RAM ÜZERİNDEN)
    // Sadece /api/v1/currencies/rates endpoint'i (Next.js ekranı) burayı kullanır.
    // ========================================================================
    @Cacheable(value = "rates", key = "#baseCurrency")
    @Override
    public ExchangeRateResponse getLiveRates(String baseCurrency) {
        log.info("VİTRİN İÇİN DIŞ API İSTEĞİ: {} kurları çekiliyor (10 dk RAM'de kalacak)...", baseCurrency.toUpperCase());
        return fetchRatesFromExternalApi(baseCurrency);
    }

    // ========================================================================
    // 2. KARARGAH OPERASYONU (ÖNBELLEKSİZ - %100 CANLI)
    // Core Service para transferi yaparken (Feign üzerinden) BURAYI KULLANIR!
    // ========================================================================
    @Override
    public Double convertAmount(Double amount, String fromCurrency, String toCurrency) {

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            log.info("Çevirim İptali: Kaynak ve hedef aynı ({}).", fromCurrency.toUpperCase());
            return amount;
        }

        log.info("PARA TRANSFERİ (CANLI): {} {} -> {} çevriliyor...", amount, fromCurrency.toUpperCase(), toCurrency.toUpperCase());

        // 🚀 DİKKAT: Artık getLiveRates() DEĞİL, doğrudan dış API'ye giden gizli metodu çağırıyoruz!
        // Böylece Cache'i tamamen bypass etmiş oluyoruz.
        ExchangeRateResponse response = fetchRatesFromExternalApi(fromCurrency);

        Double rate = response.getRates().get(toCurrency.toUpperCase());

        if (rate == null) {
            log.warn("Çevirim Başarısız: Hedef para birimi ({}) desteklenmiyor!", toCurrency.toUpperCase());
            throw new BankOperationException("Desteklenmeyen para birimi: " + toCurrency);
        }

        Double result = amount * rate;

        log.info("✅ Çevirim Başarılı (CANLI KUR İLE): {} {} = {} {} (Uygulanan Çarpan: {})",
                amount, fromCurrency.toUpperCase(), String.format("%.2f", result), toCurrency.toUpperCase(), rate);

        return result;
    }

    // ========================================================================
    // 🛠️ YARDIMCI METOD (Sadece Dış API'ye Çıkar, Cache'den Asla Haberi Yoktur)
    // ========================================================================
    private ExchangeRateResponse fetchRatesFromExternalApi(String baseCurrency) {
        String url = "https://open.er-api.com/v6/latest/" + baseCurrency.toUpperCase();

        try {
            ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);
            log.info("Canlı kurlar başarıyla çekildi. Sağlayıcı yanıt verdi.");
            return response;
        } catch (Exception e) {
            log.error("🚨 DIŞ API BAĞLANTI HATASI: Canlı kurlar çekilemedi! Hata Detayı: {}", e.getMessage());
            throw new BankOperationException("Canlı kurlar çekilirken bir hata oluştu: " + e.getMessage());
        }
    }
}