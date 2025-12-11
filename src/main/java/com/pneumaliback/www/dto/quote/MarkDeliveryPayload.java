package com.pneumaliback.www.dto.quote;

import jakarta.validation.constraints.NotNull;

public record MarkDeliveryPayload(
        @NotNull Double latitude,
        @NotNull Double longitude,
        String photoBase64,
        String signatureData,
        String deliveryNotes
) {}

