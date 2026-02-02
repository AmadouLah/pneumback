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

    /**
     * Crée une facture PayDunya et retourne le token + URL de checkout.
     * 
     * 🔁 FLUX STANDARD PAYDUNYA :
     * 1. Backend crée la facture → PayDunya retourne token + checkoutUrl
     * 2. Frontend redirige l'utilisateur vers checkoutUrl (page PayDunya hébergée
     * par eux)
     * 3. Utilisateur choisit sa méthode de paiement sur la page PayDunya
     * 4. PayDunya gère le paiement et fait un callback IPN vers
     * /api/payments/callback/paydunya
     * 5. Backend confirme automatiquement le paiement et la commande via le
     * callback
     */
    @PostMapping("/create")
    @Operation(summary = "Créer une commande avec facture Paydunya", description = "Crée une commande à partir du panier et génère une facture Paydunya. "
            +
            "Retourne un token et une URL de checkout. Redirigez l'utilisateur vers checkoutUrl " +
            "pour qu'il choisisse sa méthode de paiement sur la page PayDunya.")
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

            // Valider le montant maximum Paydunya (limite sandbox: 3 000 000 FCFA)
            BigDecimal maxAmount = new BigDecimal("3000000");
            if (order.getTotalAmount().compareTo(maxAmount) > 0) {
                throw new IllegalArgumentException(
                        String.format(
                                "Le montant total de %.0f FCFA dépasse la limite maximale de Paydunya (3 000 000 FCFA en mode sandbox). Veuillez réduire la quantité des articles ou contacter le support.",
                                order.getTotalAmount()));
            }

            // Créer la facture Paydunya
            String description = "Commande #" + order.getOrderNumber() + " - PneuMali";
            PaydunyaInvoiceResponse invoiceResponse = paydunyaService.createInvoice(
                    order.getTotalAmount(),
                    description);

            if (invoiceResponse == null || invoiceResponse.getToken() == null) {
                throw new RuntimeException("Échec de la création de la facture Paydunya");
            }

            // Créer le paiement (la relation Payment -> Order est déjà établie dans
            // PaymentService)
            Payment payment = paymentService.createPayment(
                    order,
                    PaymentMethod.BANK_CARD, // Par défaut pour paiement en ligne
                    order.getTotalAmount(),
                    invoiceResponse.getToken());

            // Construire l'URL de checkout
            String checkoutUrl = paydunyaProperties.getCheckoutBaseUrl() + "/" + invoiceResponse.getToken();

            PaymentResponse response = PaymentResponse.builder()
                    .invoiceToken(invoiceResponse.getToken())
                    .checkoutUrl(checkoutUrl)
                    .orderId(order.getId())
                    .success(true)
                    .message("Facture créée avec succès")
                    .build();

            log.info("Facture Paydunya créée pour la commande {} - Token: {}", order.getOrderNumber(),
                    invoiceResponse.getToken());
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

    /**
     * Endpoint optionnel pour SoftPay (tests uniquement).
     * 
     * ⚠️ FLUX STANDARD PAYDUNYA :
     * 1. Créer la facture via /create → reçoit checkoutUrl
     * 2. Rediriger l'utilisateur vers checkoutUrl (page PayDunya)
     * 3. PayDunya gère le paiement et fait un callback IPN vers /callback/paydunya
     * 
     * Cet endpoint SoftPay est uniquement pour les tests avec compte fictif.
     */
    @PostMapping("/make-payment")
    @Operation(summary = "Effectuer un paiement SoftPay (optionnel - tests uniquement)", description = "Effectue le paiement via Paydunya SoftPay avec les informations du compte de test. "
            +
            "⚠️ Pour le flux standard, utilisez le checkoutUrl retourné par /create et laissez PayDunya gérer le paiement.")
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
            String invoiceToken = request.getInvoiceToken();
            log.info("Requête de paiement reçue - invoiceToken: {}, email: {}, phone: {}",
                    invoiceToken, request.getCustomerEmail(), request.getPhoneNumber());

            if (invoiceToken == null || invoiceToken.isBlank()) {
                log.error("invoiceToken est null ou vide dans la requête");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Token de facture manquant dans la requête"));
            }

            log.debug("Recherche du paiement avec invoiceToken: {}", invoiceToken);
            Payment payment = paymentRepository.findByInvoiceToken(invoiceToken)
                    .orElseThrow(() -> {
                        log.warn("Paiement introuvable pour invoiceToken: {}", invoiceToken);
                        return new IllegalArgumentException("Paiement introuvable pour ce token de facture");
                    });
            log.debug("Paiement trouvé: ID={}, invoiceToken={}", payment.getId(), payment.getInvoiceToken());

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

            // Effectuer le paiement via Paydunya (Sandbox SoftPay)
            PaydunyaPaymentResponse paymentResponse = paydunyaService.makePayment(
                    request.getPhoneNumber(),
                    request.getCustomerEmail(),
                    request.getPassword(),
                    request.getInvoiceToken());

            // Mettre à jour le statut du paiement et de la commande
            if (paymentResponse.isSuccess()) {
                paymentService.updatePaymentStatus(payment.getId(), PaymentStatus.SUCCESS, request.getInvoiceToken());
                orderService.confirm(order);
                orderRepository.save(order);
                log.info("Paiement SoftPay réussi pour la commande {} - Token: {}", order.getOrderNumber(),
                        request.getInvoiceToken());
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
