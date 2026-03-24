package com.mustafa.service.impl;

import com.mustafa.dto.response.ExchangeRateResponse;
import com.mustafa.exception.BankOperationException;
import com.mustafa.service.ICurrencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j // 🚀 LOGGER AKTİF
@Service
public class CurrencyServiceImpl implements ICurrencyService {

    // Spring'in dış dünyayla konuşmasını sağlayan HTTP aracı
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ExchangeRateResponse getLiveRates(String baseCurrency) {
        // Ücretsiz ve API Key istemeyen kur sağlayıcısı
        String url = "https://open.er-api.com/v6/latest/" + baseCurrency.toUpperCase();

        log.info("Dış API İsteği: {} bazlı canlı döviz kurları çekiliyor... [URL: {}]", baseCurrency.toUpperCase(), url);

        try {
            // Dış API'ye GET isteği at ve gelen JSON'ı bizim DTO'ya dönüştür
            ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);
            log.info("Canlı kurlar başarıyla çekildi. Sağlayıcı yanıt verdi.");
            return response;
        } catch (Exception e) {
            // 🚀 EĞER API ÇÖKERSE EN BÜYÜK KANITIMIZ BURASI:
            log.error("🚨 DIŞ API BAĞLANTI HATASI: Canlı kurlar çekilemedi! Hata Detayı: {}", e.getMessage());
            throw new BankOperationException("Canlı kurlar çekilirken bir hata oluştu: " + e.getMessage());
        }
    }

    @Override
    public Double convertAmount(Double amount, String fromCurrency, String toCurrency) {

        // 1. ÖNCE AKILLI KONTROLÜ YAP (Aynıysa hiç motoru yorma)
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            log.info("Çevirim İptali: Kaynak ve hedef para birimleri aynı ({}). İşlem yapılmadı.", fromCurrency.toUpperCase());
            return amount;
        }

        // 2. EĞER FARKLIYSA MOTORU ÇALIŞTIRDIĞINI BİLDİR (🚀 Logu buraya taşıdık)
        log.info("Döviz çevirim motoru çalıştı: {} {} -> {}", amount, fromCurrency.toUpperCase(), toCurrency.toUpperCase());

        // 3. "From" para birimine göre tüm kurları çek
        ExchangeRateResponse response = getLiveRates(fromCurrency);

        // 4. Hedef para biriminin karşılığını al
        Double rate = response.getRates().get(toCurrency.toUpperCase());

        if (rate == null) {
            log.warn("Çevirim Başarısız: Hedeflenen para birimi ({}) sistemde veya dış API'de desteklenmiyor!", toCurrency.toUpperCase());
            throw new BankOperationException("Desteklenmeyen para birimi: " + toCurrency);
        }

        // 5. Miktarı kurla çarp ve dön
        Double result = amount * rate;

        log.info("✅ Çevirim Başarılı: {} {} = {} {} (Uygulanan Kur Çarpanı: {})",
                amount, fromCurrency.toUpperCase(), String.format("%.2f", result), toCurrency.toUpperCase(), rate);

        return result;
    }
}