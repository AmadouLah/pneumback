package com.pneumaliback.www.dto;

import java.math.BigDecimal;

public record DeliveryCreateDTO(
        Long orderId,
        Long addressId,
        String zone,
        BigDecimal shippingFee
) {}
