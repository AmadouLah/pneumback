package com.pneumaliback.www.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email
) {}
