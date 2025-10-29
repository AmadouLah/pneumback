package com.pneumaliback.www.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserInfo userInfo) {

    public record UserInfo(
            Long id,
            String email,
            String firstName,
            String lastName,
            String role) {
    }
}
