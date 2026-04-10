package com.mustafa.service.impl;

import com.mustafa.dto.response.ExchangeRateResponse;
import com.mustafa.exception.BankOperationException;
import com.mustafa.service.ICurrencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class CurrencyServiceImpl implements ICurrencyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ICurrencyService self; // 🛡️ KENDİMİZİ (PROXY KOPYASINI) TUTACAĞIMIZ DEĞİŞKEN

    // 🚀 @Lazy ile Spring'e diyoruz ki: "Beni bana enjekte et ama sonsuz döngüye sokma!"
    public CurrencyServiceImpl(@Lazy ICurrencyService self) {
        this.self = self;
    }

    // 🚀 KURLARI RAM'E KAYDETME EMRİ (isim: "rates", anahtar: TRY, USD vs.)
    @Cacheable(value = "rates", key = "#baseCurrency")
    @Override
    public ExchangeRateResponse getLiveRates(String baseCurrency) {
        String url = "https://open.er-api.com/v6/latest/" + baseCurrency.toUpperCase();
        log.info("Dış API İsteği: {} bazlı canlı döviz kurları çekiliyor... [URL: {}]", baseCurrency.toUpperCase(), url);

        try {
            ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);
            log.info("Canlı kurlar başarıyla çekildi. Sağlayıcı yanıt verdi.");
            return response;
        } catch (Exception e) {
            log.error("🚨 DIŞ API BAĞLANTI HATASI: Canlı kurlar çekilemedi! Hata Detayı: {}", e.getMessage());
            throw new BankOperationException("Canlı kurlar çekilirken bir hata oluştu: " + e.getMessage());
        }
    }

    @Override
    public Double convertAmount(Double amount, String fromCurrency, String toCurrency) {

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            log.info("Çevirim İptali: Kaynak ve hedef aynı ({}).", fromCurrency.toUpperCase());
            return amount;
        }

        log.info("Döviz çevirim motoru çalıştı: {} {} -> {}", amount, fromCurrency.toUpperCase(), toCurrency.toUpperCase());

        // 🚀 DİKKAT: Artık doğrudan getLiveRates() değil, self.getLiveRates() diyoruz!
        // Böylece Spring Cache araya girip "Bu veri RAM'de var mı?" diye bakabilecek.
        ExchangeRateResponse response = self.getLiveRates(fromCurrency);

        Double rate = response.getRates().get(toCurrency.toUpperCase());

        if (rate == null) {
            log.warn("Çevirim Başarısız: Hedef para birimi ({}) desteklenmiyor!", toCurrency.toUpperCase());
            throw new BankOperationException("Desteklenmeyen para birimi: " + toCurrency);
        }

        Double result = amount * rate;

        log.info("✅ Çevirim Başarılı: {} {} = {} {} (Kur: {})",
                amount, fromCurrency.toUpperCase(), String.format("%.2f", result), toCurrency.toUpperCase(), rate);

        return result;
    }
}