package com.pneumaliback.www.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO pour recevoir le callback IPN de PayDunya.
 * PayDunya envoie les données dans un objet "data" ou directement à la racine.
 * Ce DTO gère les deux formats.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaydunyaCallbackRequest {
    @JsonProperty("data")
    private CallbackData data;

    // Champs directs (si pas de wrapper "data")
    @JsonProperty("response_code")
    private String responseCode;

    @JsonProperty("response_text")
    private String responseText;

    private String hash;

    private Invoice invoice;

    @JsonProperty("custom_data")
    private Object customData;

    private String mode;

    private String status;

    private Customer customer;

    @JsonProperty("receipt_url")
    private String receiptUrl;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackData {
        @JsonProperty("response_code")
        private String responseCode;

        @JsonProperty("response_text")
        private String responseText;

        private String hash;

        private Invoice invoice;

        @JsonProperty("custom_data")
        private Object customData;

        private String mode;

        private String status;

        private Customer customer;

        @JsonProperty("receipt_url")
        private String receiptUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Invoice {
        private String token;
        @JsonProperty("total_amount")
        private String totalAmount;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        private String name;
        private String phone;
        private String email;
    }

    // Méthodes de convenance pour accéder aux données (gère les deux formats)
    public String getActualResponseCode() {
        return data != null ? data.getResponseCode() : responseCode;
    }

    public String getActualHash() {
        return data != null ? data.getHash() : hash;
    }

    public Invoice getActualInvoice() {
        return data != null ? data.getInvoice() : invoice;
    }

    public String getActualStatus() {
        return data != null ? data.getStatus() : status;
    }
}
