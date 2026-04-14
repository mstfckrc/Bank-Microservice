package com.mustafa.service;

public interface ICaptchaService {
    /**
     * Google reCAPTCHA üzerinden kullanıcının insan olup olmadığını doğrular.
     * @param captchaToken Frontend'den gelen güvenlik bileti
     * @return İnsansa true, robotsa veya hata alınırsa false döner.
     */
    boolean verifyToken(String captchaToken);
}