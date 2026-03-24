package com.mustafa.service.impl;

import com.mustafa.client.CurrencyServiceClient;
import com.mustafa.dto.response.ExchangeRateResponse;
import com.mustafa.exception.BankOperationException;
import com.mustafa.service.ICurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements ICurrencyService {

    // 🚀 ESKİ RestTemplate GİTTİ, YENİ TELSİZ (Feign) GELDİ!
    private final CurrencyServiceClient currencyServiceClient;

    @Override
    public ExchangeRateResponse getLiveRates(String baseCurrency) {
        log.info("📡 TELSİZ (FEIGN) AKTİF: Yeni kur servisine ({}) bağlanılıyor...", baseCurrency.toUpperCase());

        try {
            // Artık dış API'ye değil, kendi mikroservisimize soruyoruz!
            ExchangeRateResponse response = currencyServiceClient.getRates(baseCurrency);
            log.info("✅ Kurlar yeni mikroservisten başarıyla çekildi.");
            return response;
        } catch (Exception e) {
            log.error("🚨 YENİ KUR SERVİSİNE BAĞLANILAMADI! Hata: {}", e.getMessage());
            throw new BankOperationException("Kur servisine ulaşılamıyor: " + e.getMessage());
        }
    }

    @Override
    public Double convertAmount(Double amount, String fromCurrency, String toCurrency) {
        // 🚀 MATEMATİK KISMI AYNEN KALIYOR (TransactionService bozulmasın diye)
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        log.info("Döviz çevirim motoru çalıştı: {} {} -> {}", amount, fromCurrency.toUpperCase(), toCurrency.toUpperCase());

        // Burası artık üstteki yepyeni telsizli metodu (getLiveRates) çağırıyor!
        ExchangeRateResponse response = getLiveRates(fromCurrency);

        Double rate = response.getRates().get(toCurrency.toUpperCase());

        if (rate == null) {
            log.warn("Çevirim Başarısız: Hedeflenen para birimi ({}) desteklenmiyor!", toCurrency.toUpperCase());
            throw new BankOperationException("Desteklenmeyen para birimi: " + toCurrency);
        }

        Double result = amount * rate;
        log.info("✅ Çevirim Başarılı: {} {} = {} {}", amount, fromCurrency.toUpperCase(), String.format("%.2f", result), toCurrency.toUpperCase());

        return result;
    }
}