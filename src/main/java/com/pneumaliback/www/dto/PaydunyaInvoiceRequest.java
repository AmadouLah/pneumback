package com.pneumaliback.www.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaydunyaInvoiceRequest {
    private Invoice invoice;
    private Store store;
    private Actions actions;

    @Data
    @Builder
    public static class Invoice {
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        private String description;
    }

    @Data
    @Builder
    public static class Store {
        private String name;
    }

    @Data
    @Builder
    public static class Actions {
        @JsonProperty("callback_url")
        private String callbackUrl;
    }
}
