package com.pneumaliback.www.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateInfluenceurRequest(
        @NotBlank(message = "Le prénom est requis") @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères") String firstName,

        @NotBlank(message = "Le nom est requis") @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères") String lastName,

        @NotBlank(message = "L'email est requis") @Email(message = "L'email doit être valide") @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères") String email,

        @NotNull(message = "Le taux de commission est requis") @DecimalMin(value = "0.01", message = "Le taux de commission doit être supérieur à 0") @DecimalMax(value = "100.00", message = "Le taux de commission ne peut pas dépasser 100%") BigDecimal commissionRate,

        Boolean active) {
}
