package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.Country;

public record AddressSimpleDTO(
        Long id,
        String street,
        String city,
        String region,
        Country country,
        String postalCode
) {}
