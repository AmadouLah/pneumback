package com.pneumaliback.www.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.pneumaliback.www.validation.StrongPassword;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères") String firstName,

        @NotBlank(message = "Le prénom est obligatoire") @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères") String lastName,

        @NotBlank(message = "L'email est obligatoire") @Email(message = "Format d'email invalide") String email,

        @NotBlank(message = "Le mot de passe est obligatoire") @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") @StrongPassword String password,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire") String confirmPassword,

        String phoneNumber) {
}
