package com.pneumaliback.www.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le code est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code doit contenir 6 chiffres")
        String code
) {}
