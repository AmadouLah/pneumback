package com.pneumaliback.www.dto;

import java.time.LocalDateTime;

import com.pneumaliback.www.enums.Country;

public record AddressDTO(
        Long id,
        String street,
        String city,
        String region,
        Country country,
        String postalCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long utilisateurId,
        String utilisateurNom,
        String utilisateurPrenom
) {}
