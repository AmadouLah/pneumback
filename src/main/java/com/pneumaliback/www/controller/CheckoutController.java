package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.AddressRepository;
import com.pneumaliback.www.repository.OrderRepository;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Checkout", description = "Création de commandes à partir du panier")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;

    public record CheckoutRequest(Long userId, Long addressId, String zone, String promoCode) {}

    @PostMapping
    @Operation(summary = "Créer une commande à partir du panier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commande créée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou adresse introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createOrder(@RequestBody CheckoutRequest req) {
        try {
            if (req == null || req.userId() == null || req.addressId() == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Paramètres de checkout invalides"));
            }
            User user = userRepository.findById(req.userId())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            Address address = addressRepository.findById(req.addressId())
                    .orElseThrow(() -> new IllegalArgumentException("Adresse non trouvée"));
            Order order = checkoutService.createOrder(user, address, req.zone(), req.promoCode());
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
        }
    }
}

