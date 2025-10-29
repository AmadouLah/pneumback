package com.pneumaliback.www.exception;

public class CodeVerificationRequiredException extends RuntimeException {
    public CodeVerificationRequiredException(String message) {
        super(message);
    }
}
