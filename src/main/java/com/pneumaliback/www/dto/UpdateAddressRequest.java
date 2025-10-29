package com.pneumaliback.www.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la mise à jour d'une adresse
 * Tous les champs sont optionnels
 * Seuls les champs fournis seront mis à jour
 * Les validations s'appliquent uniquement si le champ est fourni
 */
public record UpdateAddressRequest(
                @Size(min = 5, max = 150, message = "L'adresse doit contenir entre 5 et 150 caractères") String street,

                @Size(min = 2, max = 100, message = "La ville doit contenir entre 2 et 100 caractères") String city,

                @Size(max = 100, message = "La région ne doit pas dépasser 100 caractères") String region,

                String country,

                @Size(max = 50, message = "Le code postal ne doit pas dépasser 50 caractères") String postalCode,

                @Pattern(regexp = "^[0-9]{8,15}$", message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres") String phoneNumber,

                Boolean isDefault) {
}
