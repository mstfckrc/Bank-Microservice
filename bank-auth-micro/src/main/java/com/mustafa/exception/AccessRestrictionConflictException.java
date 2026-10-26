package com.mustafa.exception;

import lombok.Getter;

@Getter
public class AccessRestrictionConflictException extends RuntimeException {
    private final String code;

    public AccessRestrictionConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
