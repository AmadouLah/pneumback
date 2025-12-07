package com.pneumaliback.www.dto;

public record UserSimpleDTO(
                Long id,
                String email,
                String firstName,
                String lastName,
                Boolean enabled) {
}
