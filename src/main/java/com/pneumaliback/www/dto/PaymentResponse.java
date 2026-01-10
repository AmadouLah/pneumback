package com.pneumaliback.www.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String invoiceToken;
    private String checkoutUrl;
    private Long orderId;
    private boolean success;
    private String message;
}
