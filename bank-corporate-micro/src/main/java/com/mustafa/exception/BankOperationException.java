package com.mustafa.exception;

public class BankOperationException extends RuntimeException {
    public BankOperationException(String message) {
        super(message);
    }
}