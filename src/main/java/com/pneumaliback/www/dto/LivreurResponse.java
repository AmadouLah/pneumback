package com.pneumaliback.www.dto;

public record LivreurResponse(
                Long id,
                UserInfo user) {
        public record UserInfo(
                        Long id,
                        String email,
                        String firstName,
                        String lastName,
                        Boolean enabled) {
        }
}
