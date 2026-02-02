package com.pneumaliback.www.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaydunyaPaymentRequest {
    @JsonProperty("customer_name")
    @JsonAlias({ "customerName", "customer_name" })
    private String customerName;

    @JsonProperty("phone_phone")
    @JsonAlias({ "phoneNumber", "phone_number" })
    private String phoneNumber;

    @JsonProperty("customer_email")
    @JsonAlias({ "customerEmail", "customer_email" })
    private String customerEmail;

    private String password;

    @JsonProperty("invoice_token")
    @JsonAlias({ "invoiceToken", "invoice_token" })
    private String invoiceToken;
}
