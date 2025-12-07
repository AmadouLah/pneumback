package com.pneumaliback.www.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLivreurRequest(
                @NotBlank(message = "Le prénom est requis") @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères") String firstName,

                @NotBlank(message = "Le nom est requis") @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères") String lastName,

                @Email(message = "L'email doit être valide") @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères") String email) {
}
