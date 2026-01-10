package com.pneumaliback.www.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaydunyaPaymentRequest {
    @JsonProperty("phone_phone")
    private String phoneNumber;

    @JsonProperty("customer_email")
    private String customerEmail;

    private String password;

    @JsonProperty("invoice_token")
    private String invoiceToken;
}
