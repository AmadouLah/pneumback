package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.Country;

import jakarta.validation.constraints.Size;

public record ModifierAddressDTO(
        @Size(max = 150, message = "La rue ne peut pas dépasser 150 caractères")
        String street,
        
        @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
        String city,
        
        @Size(max = 100, message = "La région ne peut pas dépasser 100 caractères")
        String region,
        
        Country country,
        
        @Size(max = 50, message = "Le code postal ne peut pas dépasser 50 caractères")
        String postalCode
) {}
