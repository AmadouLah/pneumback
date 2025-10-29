package com.pneumaliback.www.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO pour l'ajout d'adresse
 * Champs obligatoires selon les standards e-commerce :
 * - Pays, Adresse, Ville, Région, Numéro de téléphone
 */
public record AddAddressRequest(
                @NotBlank(message = "L'adresse est obligatoire") @Size(min = 5, max = 150, message = "L'adresse doit contenir entre 5 et 150 caractères") String street,

                @NotBlank(message = "La ville est obligatoire") @Size(min = 2, max = 100, message = "La ville doit contenir entre 2 et 100 caractères") String city,

                @NotBlank(message = "La région est obligatoire") @Size(max = 100, message = "La région ne doit pas dépasser 100 caractères") String region,

                @NotBlank(message = "Le pays est obligatoire") String country,

                @Size(max = 50, message = "Le code postal ne doit pas dépasser 50 caractères") String postalCode,

                @NotBlank(message = "Le numéro de téléphone est obligatoire") @Pattern(regexp = "^[0-9]{8,15}$", message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres") String phoneNumber,

                boolean isDefault) {
}
