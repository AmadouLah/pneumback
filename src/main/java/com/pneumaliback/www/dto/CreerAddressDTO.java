package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.Country;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreerAddressDTO(
        @NotNull(message = "L'ID utilisateur est obligatoire")
        Long utilisateurId,
        
        @NotBlank(message = "La rue est obligatoire")
        @Size(max = 150, message = "La rue ne peut pas dépasser 150 caractères")
        String street,
        
        @NotBlank(message = "La ville est obligatoire")
        @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
        String city,
        
        @NotBlank(message = "La région est obligatoire")
        @Size(max = 100, message = "La région ne peut pas dépasser 100 caractères")
        String region,
        
        @NotNull(message = "Le pays est obligatoire")
        Country country,
        
        @Size(max = 50, message = "Le code postal ne peut pas dépasser 50 caractères")
        String postalCode
) {}