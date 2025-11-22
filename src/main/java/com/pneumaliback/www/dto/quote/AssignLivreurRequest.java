package com.pneumaliback.www.dto.quote;

import jakarta.validation.constraints.NotNull;

public record AssignLivreurRequest(
        @NotNull Long livreurId,
        String deliveryDetails) {
}
