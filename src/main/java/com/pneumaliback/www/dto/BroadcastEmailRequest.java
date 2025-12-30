package com.pneumaliback.www.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastEmailRequest(
                @NotBlank(message = "Le sujet est obligatoire") @Size(min = 3, max = 200, message = "Le sujet doit contenir entre 3 et 200 caractères") String subject,

                @NotBlank(message = "Le message est obligatoire") @Size(min = 10, max = 10000, message = "Le message doit contenir entre 10 et 10000 caractères") String message) {
}
