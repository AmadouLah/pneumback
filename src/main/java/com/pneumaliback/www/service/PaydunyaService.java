package com.pneumaliback.www.service;

import com.pneumaliback.www.configuration.PaydunyaProperties;
import com.pneumaliback.www.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaydunyaService {

    private final RestTemplate restTemplate;
    private final PaydunyaProperties paydunyaProperties;

    // ObjectMapper local avec configuration minimale pour la désérialisation
    // Paydunya
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public PaydunyaInvoiceResponse createInvoice(BigDecimal totalAmount, String description) {
        String url = paydunyaProperties.getApiBaseUrl() + "/checkout-invoice/create";

        PaydunyaInvoiceRequest request = PaydunyaInvoiceRequest.builder()
                .invoice(PaydunyaInvoiceRequest.Invoice.builder()
                        .totalAmount(totalAmount)
                        .description(description != null ? description : "Paiement PneuMali")
                        .build())
                .store(PaydunyaInvoiceRequest.Store.builder()
                        .name(paydunyaProperties.getStoreName())
                        .build())
                .build();

        HttpHeaders headers = createHeaders();
        HttpEntity<PaydunyaInvoiceRequest> entity = new HttpEntity<>(request, headers);

        String responseBody = null;
        try {
            log.info("Création de facture Paydunya - Montant: {}, URL: {}", totalAmount, url);
            log.debug("Requête Paydunya - Invoice: totalAmount={}, description={}, storeName={}",
                    request.getInvoice().getTotalAmount(),
                    request.getInvoice().getDescription(),
                    request.getStore().getName());

            // Récupérer la réponse brute d'abord pour le debugging
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            responseBody = rawResponse.getBody();
            log.debug("Réponse brute de Paydunya: {}", responseBody);

            if (responseBody == null || responseBody.isBlank()) {
                log.error("Réponse vide de Paydunya");
                throw new RuntimeException("Réponse vide de Paydunya");
            }

            // Désérialiser la réponse JSON
            PaydunyaInvoiceResponse invoiceResponse = OBJECT_MAPPER.readValue(responseBody,
                    PaydunyaInvoiceResponse.class);

            if (invoiceResponse == null) {
                log.error("Impossible de désérialiser la réponse Paydunya. Réponse brute: {}", responseBody);
                throw new RuntimeException("Impossible de désérialiser la réponse Paydunya");
            }

            log.debug("Réponse Paydunya désérialisée - Code: {}, Text: {}, Token: {}",
                    invoiceResponse.getResponseCode(), invoiceResponse.getResponseText(), invoiceResponse.getToken());

            // Vérifier le code de réponse Paydunya
            if (!"00".equals(invoiceResponse.getResponseCode())) {
                String errorMsg = invoiceResponse.getResponseText() != null
                        ? invoiceResponse.getResponseText()
                        : "Code de réponse invalide: " + invoiceResponse.getResponseCode();
                log.error("Erreur Paydunya lors de la création de la facture: {} - {} - Réponse complète: {}",
                        invoiceResponse.getResponseCode(), errorMsg, responseBody);
                throw new RuntimeException("Erreur Paydunya: " + errorMsg);
            }

            if (invoiceResponse.getToken() == null || invoiceResponse.getToken().isBlank()) {
                log.error("Token manquant dans la réponse Paydunya. Response complète: {} - Body brut: {}",
                        invoiceResponse, responseBody);
                throw new RuntimeException(
                        "Token manquant dans la réponse Paydunya. Response code: " + invoiceResponse.getResponseCode());
            }

            log.info("Facture Paydunya créée avec succès. Token: {}", invoiceResponse.getToken());
            return invoiceResponse;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Erreur HTTP lors de la création de la facture Paydunya: {} - Status: {} - Body: {}",
                    url, e.getStatusCode(), errorBody);
            throw new RuntimeException("Erreur lors de la création de la facture Paydunya: " + errorBody, e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Erreur de désérialisation JSON de la réponse Paydunya: {} - Réponse brute: {}",
                    e.getMessage(), responseBody);
            throw new RuntimeException("Erreur lors de la désérialisation de la réponse Paydunya: " + e.getMessage(),
                    e);
        } catch (RuntimeException e) {
            // Ré-émettre les RuntimeException (nos erreurs métier)
            throw e;
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la facture Paydunya", e);
            throw new RuntimeException(
                    "Erreur inattendue lors de la création de la facture Paydunya: " + e.getMessage(), e);
        }
    }

    public PaydunyaPaymentResponse makePayment(String phoneNumber, String customerEmail, String password,
            String invoiceToken) {
        String url = paydunyaProperties.getApiBaseUrl() + "/softpay/checkout/make-payment";

        PaydunyaPaymentRequest request = PaydunyaPaymentRequest.builder()
                .phoneNumber(phoneNumber)
                .customerEmail(customerEmail)
                .password(password)
                .invoiceToken(invoiceToken)
                .build();

        HttpHeaders headers = createHeaders();
        HttpEntity<PaydunyaPaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.debug("Tentative de paiement SoftPay pour la facture: {}", invoiceToken);
            ResponseEntity<PaydunyaPaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaydunyaPaymentResponse.class);

            PaydunyaPaymentResponse paymentResponse = response.getBody();
            if (paymentResponse == null) {
                throw new RuntimeException("Réponse invalide de Paydunya pour le paiement");
            }

            if (paymentResponse.isSuccess()) {
                log.info("Paiement SoftPay effectué avec succès pour la facture: {}", invoiceToken);
            } else {
                log.warn("Paiement SoftPay échoué pour la facture: {}. Message: {}", invoiceToken,
                        paymentResponse.getMessage());
            }

            return paymentResponse;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erreur lors du paiement SoftPay: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            // Essayer d'extraire le message d'erreur de la réponse
            String errorMessage = "Erreur lors du paiement SoftPay";
            try {
                if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty()) {
                    // On pourrait parser la réponse JSON ici si nécessaire
                    errorMessage = "Erreur Paydunya: " + e.getResponseBodyAsString();
                }
            } catch (Exception ignored) {
                // Si on ne peut pas lire le body, utiliser le message par défaut
            }

            PaydunyaPaymentResponse errorResponse = new PaydunyaPaymentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(errorMessage);
            return errorResponse;
        } catch (Exception e) {
            log.error("Erreur inattendue lors du paiement SoftPay", e);
            PaydunyaPaymentResponse errorResponse = new PaydunyaPaymentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Erreur inattendue lors du paiement: " + e.getMessage());
            return errorResponse;
        }
    }

    private HttpHeaders createHeaders() {
        String masterKey = paydunyaProperties.getMasterKey();
        String privateKey = paydunyaProperties.getPrivateKey();
        String token = paydunyaProperties.getToken();

        if (masterKey == null || masterKey.isBlank()) {
            String errorMsg = "ERREUR CRITIQUE: paydunya.master-key n'est pas configurée ou est vide. Vérifiez application.properties";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        if (privateKey == null || privateKey.isBlank()) {
            String errorMsg = "ERREUR CRITIQUE: paydunya.private-key n'est pas configurée ou est vide. Vérifiez application.properties";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        if (token == null || token.isBlank()) {
            String errorMsg = "ERREUR CRITIQUE: paydunya.token n'est pas configurée ou est vide. Vérifiez application.properties";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Nettoyer les clés (trim pour enlever les espaces)
        String cleanMasterKey = masterKey.trim();
        String cleanPrivateKey = privateKey.trim();
        String cleanToken = token.trim();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("PAYDUNYA-MASTER-KEY", cleanMasterKey);
        headers.set("PAYDUNYA-PRIVATE-KEY", cleanPrivateKey);
        headers.set("PAYDUNYA-TOKEN", cleanToken);

        log.info(
                "Headers Paydunya configurés - MasterKey: {} chars (début: {}...{}), PrivateKey: {} chars, Token: {} chars",
                cleanMasterKey.length(),
                cleanMasterKey.substring(0, Math.min(8, cleanMasterKey.length())),
                cleanMasterKey.substring(Math.max(0, cleanMasterKey.length() - 8)),
                cleanPrivateKey.length(),
                cleanToken.length());

        return headers;
    }
}
