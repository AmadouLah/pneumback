package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Cart;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.CartService;
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
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Panier", description = "Gestion du panier utilisateur")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    }

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg));
        }
        return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping
    @Operation(summary = "Obtenir le panier d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Panier récupéré", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getOrCreate(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(cartService.getOrCreate(getUser(userId)));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/items")
    @Operation(summary = "Ajouter un article au panier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Article ajouté", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou produit non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> addItem(@RequestParam Long userId,
                                        @RequestParam Long productId,
                                        @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(cartService.addItem(getUser(userId), productId, quantity));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/items")
    @Operation(summary = "Mettre à jour la quantité d'un article")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Article mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou produit non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateItem(@RequestParam Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(cartService.updateItem(getUser(userId), productId, quantity));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping
    @Operation(summary = "Vider le panier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Panier vidé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> clear(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(cartService.clear(getUser(userId)));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

