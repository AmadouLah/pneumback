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

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaydunyaService {

    private final RestTemplate restTemplate;
    private final PaydunyaProperties paydunyaProperties;

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

        try {
            log.debug("Création de facture Paydunya pour un montant de {}", totalAmount);
            ResponseEntity<PaydunyaInvoiceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaydunyaInvoiceResponse.class);

            PaydunyaInvoiceResponse invoiceResponse = response.getBody();
            if (invoiceResponse == null || invoiceResponse.getToken() == null) {
                throw new RuntimeException("Réponse invalide de Paydunya : token manquant");
            }

            log.info("Facture Paydunya créée avec succès. Token: {}", invoiceResponse.getToken());
            return invoiceResponse;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erreur lors de la création de la facture Paydunya: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException("Erreur lors de la création de la facture Paydunya: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la facture Paydunya", e);
            throw new RuntimeException("Erreur inattendue lors de la création de la facture Paydunya", e);
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("PAYDUNYA-MASTER-KEY", paydunyaProperties.getMasterKey());
        headers.set("PAYDUNYA-PRIVATE-KEY", paydunyaProperties.getPrivateKey());
        headers.set("PAYDUNYA-TOKEN", paydunyaProperties.getToken());
        return headers;
    }
}
