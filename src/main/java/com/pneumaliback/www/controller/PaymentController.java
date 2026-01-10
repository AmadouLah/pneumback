package com.pneumaliback.www.controller;

import com.pneumaliback.www.configuration.PaydunyaProperties;
import com.pneumaliback.www.dto.*;
import com.pneumaliback.www.entity.*;
import com.pneumaliback.www.enums.PaymentMethod;
import com.pneumaliback.www.enums.PaymentStatus;
import com.pneumaliback.www.repository.*;
import com.pneumaliback.www.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Paiements", description = "Gestion des paiements en ligne via Paydunya SoftPay")
public class PaymentController {

    private final PaydunyaService paydunyaService;
    private final PaymentService paymentService;
    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaydunyaProperties paydunyaProperties;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @PostMapping("/create")
    @Operation(summary = "Créer une commande avec facture Paydunya", description = "Crée une commande à partir du panier et génère une facture Paydunya pour le paiement en ligne")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commande et facture créées avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou adresse introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    @Transactional
    public ResponseEntity<?> createPayment(@AuthenticationPrincipal UserDetails userDetails,
                                          @Valid @RequestBody CreatePaymentRequest request) {
        try {
            User user = resolveUser(userDetails);
            
            Address address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("Adresse introuvable"));

            // Synchroniser le panier avec les items fournis
            if (request.getCartItems() == null || request.getCartItems().isEmpty()) {
                throw new IllegalArgumentException("Le panier ne peut pas être vide");
            }
            cartService.syncCartItems(user, request.getCartItems());

            // Créer la commande
            Order order = checkoutService.createOrder(user, address, request.getZone(), request.getPromoCode());
            
            // Créer la facture Paydunya
            String description = "Commande #" + order.getOrderNumber() + " - PneuMali";
            PaydunyaInvoiceResponse invoiceResponse = paydunyaService.createInvoice(
                    order.getTotalAmount(),
                    description
            );

            if (invoiceResponse == null || invoiceResponse.getToken() == null) {
                throw new RuntimeException("Échec de la création de la facture Paydunya");
            }

            // Créer le paiement
            Payment payment = paymentService.createPayment(
                    order,
                    PaymentMethod.BANK_CARD, // Par défaut pour paiement en ligne
                    order.getTotalAmount(),
                    invoiceResponse.getToken()
            );

            order.setPayment(payment);
            orderRepository.save(order);

            // Construire l'URL de checkout
            String checkoutUrl = paydunyaProperties.getCheckoutBaseUrl() + "/" + invoiceResponse.getToken();

            PaymentResponse response = PaymentResponse.builder()
                    .invoiceToken(invoiceResponse.getToken())
                    .checkoutUrl(checkoutUrl)
                    .orderId(order.getId())
                    .success(true)
                    .message("Facture créée avec succès")
                    .build();

            log.info("Facture Paydunya créée pour la commande {} - Token: {}", order.getOrderNumber(), invoiceResponse.getToken());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation lors de la création du paiement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de la création du paiement", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la création du paiement", "message", e.getMessage()));
        }
    }

    @PostMapping("/make-payment")
    @Operation(summary = "Effectuer un paiement SoftPay", description = "Effectue le paiement via Paydunya SoftPay avec les informations du compte de test")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paiement traité", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaydunyaPaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Commande ou paiement introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    @Transactional
    public ResponseEntity<?> makePayment(@AuthenticationPrincipal UserDetails userDetails,
                                        @Valid @RequestBody PaydunyaPaymentRequest request) {
        try {
            User user = resolveUser(userDetails);

            // Trouver le paiement par invoiceToken
            Payment payment = paymentRepository.findByInvoiceToken(request.getInvoiceToken())
                    .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable pour ce token de facture"));

            Order order = payment.getOrder();
            if (order == null) {
                throw new IllegalArgumentException("Commande introuvable pour ce paiement");
            }

            // Vérifier que le paiement n'est pas déjà complété
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Cette facture a déjà été réglée"));
            }

            // Vérifier que l'utilisateur est le propriétaire de la commande
            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Accès non autorisé à cette commande"));
            }

            // Effectuer le paiement via Paydunya
            PaydunyaPaymentResponse paymentResponse = paydunyaService.makePayment(
                    request.getPhoneNumber(),
                    request.getCustomerEmail(),
                    request.getPassword(),
                    request.getInvoiceToken()
            );

            // Mettre à jour le statut du paiement et de la commande
            if (paymentResponse.isSuccess()) {
                paymentService.updatePaymentStatus(payment.getId(), PaymentStatus.SUCCESS, request.getInvoiceToken());
                orderService.confirm(order);
                orderRepository.save(order);
                log.info("Paiement SoftPay réussi pour la commande {} - Token: {}", order.getOrderNumber(), request.getInvoiceToken());
            } else {
                paymentService.updatePaymentStatus(payment.getId(), PaymentStatus.FAILED, null);
                log.warn("Paiement SoftPay échoué pour la commande {} - Token: {} - Message: {}", 
                        order.getOrderNumber(), request.getInvoiceToken(), paymentResponse.getMessage());
            }

            return ResponseEntity.ok(paymentResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation lors du paiement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors du paiement SoftPay", e);
            PaydunyaPaymentResponse errorResponse = new PaydunyaPaymentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Erreur lors du paiement: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private User resolveUser(UserDetails principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }
        return userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }
}
