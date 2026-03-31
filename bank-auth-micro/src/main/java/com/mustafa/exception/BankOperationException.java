package com.mustafa.exception;

// Bankacılık işlemleri (bakiye yetersiz, hesap başkasının vb.) sırasındaki hatalar için
public class BankOperationException extends RuntimeException {
    public BankOperationException(String message) {
        super(message);
    }
}