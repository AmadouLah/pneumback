package com.pneumaliback.www.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    @NotNull(message = "L'adresse de livraison est requise")
    private Long addressId;

    @NotBlank(message = "La zone de livraison est requise")
    private String zone;

    private String promoCode;
}
