package com.pneumaliback.www.dto.quote;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateQuoteRequestPayload(
        @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères") String message,
        @NotEmpty(message = "Le panier ne peut pas être vide") @Valid List<Item> items) {

    public record Item(
            @NotNull(message = "L'identifiant produit est requis") Long productId,
            @Positive(message = "La quantité doit être supérieure à zéro") Integer quantity) {
    }
}
