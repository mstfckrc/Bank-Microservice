package com.mustafa.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

public class BankAccessException extends AccessDeniedException {
    private final String code;
    private final HttpStatus status;

    public BankAccessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
