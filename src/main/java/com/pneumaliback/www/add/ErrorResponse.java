package com.pneumaliback.www.add;

public class ErrorResponse {
    private final String message;
    private final boolean success;

    public ErrorResponse(String message) {
        this.message = message;
        this.success = false;
    }

    public ErrorResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }
}
