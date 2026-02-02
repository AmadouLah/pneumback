package com.pneumaliback.www.controller;

import com.pneumaliback.www.configuration.PaydunyaProperties;
import com.pneumaliback.www.dto.PaydunyaCallbackRequest;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.entity.Payment;
import com.pneumaliback.www.enums.PaymentStatus;
import com.pneumaliback.www.repository.PaymentRepository;
import com.pneumaliback.www.service.OrderService;
import com.pneumaliback.www.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/callback")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Paiements - Callback", description = "Callbacks des prestataires de paiement")
public class PaymentCallbackController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaydunyaProperties paydunyaProperties;

    @PostMapping("/paydunya")
    @Operation(summary = "Callback IPN PayDunya", description = "Reçoit la notification instantanée de paiement (IPN) de PayDunya après un paiement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Callback traité avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou hash invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    @Transactional
    public ResponseEntity<?> handlePaydunyaCallback(@RequestBody PaydunyaCallbackRequest callback) {
        try {
            String actualStatus = callback.getActualStatus();
            PaydunyaCallbackRequest.Invoice actualInvoice = callback.getActualInvoice();
            String invoiceToken = actualInvoice != null ? actualInvoice.getToken() : null;

            log.info("Callback PayDunya reçu - Status: {}, Token: {}", actualStatus, invoiceToken);

            // Vérifier le hash pour s'assurer que le callback provient bien de PayDunya
            if (!verifyHash(callback)) {
                log.warn("Hash invalide dans le callback PayDunya - Possible tentative de fraude");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Hash invalide - Callback non authentifié"));
            }

            if (invoiceToken == null || invoiceToken.isBlank()) {
                log.error("Token de facture manquant dans le callback PayDunya");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Token de facture manquant"));
            }
            Payment payment = paymentRepository.findByInvoiceToken(invoiceToken)
                    .orElse(null);

            if (payment == null) {
                log.warn("Paiement introuvable pour le token: {}", invoiceToken);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Paiement introuvable pour ce token"));
            }

            // Traiter le statut du paiement
            if ("completed".equals(actualStatus)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                if (invoiceToken != null) {
                    payment.setTransactionReference(invoiceToken);
                }
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                if (order != null) {
                    orderService.confirm(order);
                }

                log.info("Paiement confirmé via callback PayDunya - Token: {}, Commande: {}",
                        invoiceToken, order != null ? order.getOrderNumber() : "N/A");
            } else if ("failed".equals(actualStatus) || "cancelled".equals(actualStatus)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.warn("Paiement échoué/annulé via callback PayDunya - Token: {}, Status: {}",
                        invoiceToken, actualStatus);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Callback traité avec succès"));

        } catch (Exception e) {
            log.error("Erreur lors du traitement du callback PayDunya", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
        }
    }

    @PostMapping("/success")
    @Operation(summary = "Callback succès paiement (legacy)", description = "Confirme le paiement et la commande via la référence transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Callback traité"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> handleSuccessCallback(@RequestParam("txRef") String transactionReference) {
        try {
            paymentService.confirmSuccessByTransaction(transactionReference);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Requête invalide"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
        }
    }

    private boolean verifyHash(PaydunyaCallbackRequest callback) {
        try {
            if (callback.getHash() == null || callback.getHash().isBlank()) {
                log.warn("Hash manquant dans le callback PayDunya");
                return false;
            }

            String masterKey = paydunyaProperties.getMasterKey();
            if (masterKey == null || masterKey.isBlank()) {
                log.error("MasterKey manquante pour la vérification du hash");
                return false;
            }

            // Calculer le hash SHA-512 de la MasterKey
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(masterKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashString = new StringBuilder();
            for (byte b : hashBytes) {
                hashString.append(String.format("%02x", b));
            }

            // Comparer avec le hash reçu
            String receivedHash = callback.getActualHash();
            boolean isValid = hashString.toString().equals(receivedHash);
            if (!isValid) {
                log.warn("Hash invalide - Attendu: {}...{}, Reçu: {}",
                        hashString.substring(0, Math.min(16, hashString.length())),
                        hashString.substring(Math.max(0, hashString.length() - 16)),
                        receivedHash != null && receivedHash.length() > 32
                                ? receivedHash.substring(0, 16) + "..." + receivedHash.substring(receivedHash.length() - 16)
                                : receivedHash);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du hash PayDunya", e);
            return false;
        }
    }
}

