package com.mustafa.dto.enums;

public enum TransactionType {
    DEPOSIT,     // Para Yatırma
    WITHDRAWAL,  // Para Çekme
    TRANSFER,    // Havale / EFT
    SALARY,      // MAAŞ (Bizim için en kritiği)
    BILL_PAYMENT // Fatura Ödemesi
}