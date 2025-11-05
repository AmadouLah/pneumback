package com.pneumaliback.www.dto;

import java.math.BigDecimal;

public record InfluenceurResponse(
        Long id,
        BigDecimal commissionRate,
        UserInfo user) {
    public record UserInfo(
            Long id,
            String email,
            String firstName,
            String lastName,
            Boolean enabled) {
    }
}
