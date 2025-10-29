package com.pneumaliback.www.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(
        @NotBlank(message = "L'email est obligatoire") String email,
        @NotBlank(message = "Le code est obligatoire") String code
) {}
