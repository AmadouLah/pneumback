package com.pneumaliback.www.exception;

public class TwoFactorRequiredException extends RuntimeException {
    public TwoFactorRequiredException(String message) {
        super(message);
    }
}
