package com.pneumaliback.www.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.pneumaliback.www.validation.StrongPassword;

public record ResetPasswordRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le code est obligatoire")
        String code,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @StrongPassword
        String newPassword,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword
) {}
