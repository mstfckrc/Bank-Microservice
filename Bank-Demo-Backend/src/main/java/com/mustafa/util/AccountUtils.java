package com.mustafa.util;

import java.util.Random;

public class AccountUtils {

    // 10 haneli rastgele bir hesap numarası üretir
    public static String generateAccountNumber() {
        Random random = new Random();
        // 1000000000 ile 9999999999 arasında bir sayı
        long number = 1000000000L + (long) (random.nextDouble() * 8999999999L);
        return String.valueOf(number);
    }

    // Üretilen hesap numarasını alıp 26 haneli standart bir TR IBAN'ına çevirir
    public static String generateIban(String accountNumber) {
        Random random = new Random();
        int checkDigit = random.nextInt(90) + 10; // 10 ile 99 arası doğrulama kodu
        String bankCode = "00099"; // Bankamızın hayali kodu

        // TR(2) + Doğrulama(2) + BankaKodu(5) + Rezerv(1) + Şube/Ek(6) + HesapNo(10) = 26 Karakter
        return "TR" + checkDigit + bankCode + "0" + "000000" + accountNumber;
    }
}