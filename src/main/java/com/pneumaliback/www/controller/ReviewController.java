package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Review;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.ReviewService;
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
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Avis", description = "Gestion des avis produits")
public class ReviewController {

    private final ReviewService reviewService;
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

    @GetMapping("/product/{productId}")
    @Operation(summary = "Lister les avis d'un produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listByProduct(@PathVariable Long productId) {
        try {
            return ResponseEntity.ok(reviewService.listByProduct(productId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    public record AddReviewRequest(Long userId, int rating, String comment) {}

    @PostMapping("/product/{productId}")
    @Operation(summary = "Ajouter un avis à un produit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avis ajouté", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou produit introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> add(@PathVariable Long productId, @RequestBody AddReviewRequest req) {
        try {
            if (req == null || req.userId() == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Paramètres invalides"));
            }
            return ResponseEntity.ok(reviewService.addReview(getUser(req.userId()), productId, req.rating(), req.comment()));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

