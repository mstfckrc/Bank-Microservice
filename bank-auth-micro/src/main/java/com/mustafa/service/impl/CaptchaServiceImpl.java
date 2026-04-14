package com.mustafa.service.impl;

import com.mustafa.service.ICaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class CaptchaServiceImpl implements ICaptchaService {

    @Value("${recaptcha.secret}") // application.yml'den alacağız
    private String recaptchaSecret;

    private static final String GOOGLE_RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Override
    public boolean verifyToken(String captchaToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Google'a gönderilecek gizli zarf (Bizim sırrımız ve adamın bileti)
            MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
            requestMap.add("secret", recaptchaSecret);
            requestMap.add("response", captchaToken);

            // Google'a telsiz at!
            Map<String, Object> apiResponse = restTemplate.postForObject(GOOGLE_RECAPTCHA_VERIFY_URL, requestMap, Map.class);

            if (apiResponse != null && (Boolean) apiResponse.get("success")) {
                log.info("Siber Kalkan: Kullanıcı doğrulandı (İnsan).");
                return true;
            } else {
                log.warn("Siber Kalkan Uyarısı: Geçersiz veya süresi dolmuş Captcha! Google Cevabı: {}", apiResponse);
                return false;
            }
        } catch (Exception e) {
            log.error("Google Captcha servisine ulaşılamadı! Hata: {}", e.getMessage());
            // Sistem Google'a ulaşamazsa kimseyi içeri alma (Güvenlik önceliği)
            return false;
        }
    }
}