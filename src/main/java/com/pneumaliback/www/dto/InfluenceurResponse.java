package com.pneumaliback.www.dto;

import java.math.BigDecimal;

public record InfluenceurResponse(
                Long id,
                BigDecimal commissionRate,
                boolean archived,
                UserInfo user) {
        public record UserInfo(
                        Long id,
                        String email,
                        String firstName,
                        String lastName,
                        Boolean enabled) {
        }
}
