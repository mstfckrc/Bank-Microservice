package com.mustafa.exception;

import org.springframework.http.HttpStatus;

public class SalaryPaymentException extends RuntimeException {
    private final HttpStatus status;

    public SalaryPaymentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
