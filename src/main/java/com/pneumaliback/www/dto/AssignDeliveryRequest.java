package com.pneumaliback.www.dto;

import jakarta.validation.constraints.NotNull;

public record AssignDeliveryRequest(
        @NotNull Long livreurId) {
}
