package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.dto.PromotionCreateDTO;
import com.pneumaliback.www.dto.PromotionResponse;
import com.pneumaliback.www.dto.PromotionValidationResponse;
import com.pneumaliback.www.dto.UpdatePromotionRequest;
import com.pneumaliback.www.service.PromotionService;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Promotions", description = "Validation et gestion des promotions")
public class PromotionController {

    private final PromotionService promotionService;

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg));
        }
        return ResponseEntity.internalServerError()
                .body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping("/validate")
    @Operation(summary = "Valider une promotion par code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion valide", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Promotion.class))),
            @ApiResponse(responseCode = "404", description = "Promotion introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> validate(@RequestParam String code) {
        try {
            Optional<Promotion> promo = promotionService.findValidByCode(code);
            return promo.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(java.util.Map.of("error", "Promotion introuvable")));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/validate-discount")
    @Operation(summary = "Valider un code promo et calculer la réduction", description = "Valide un code promo et calcule le montant de la réduction pour un sous-total donné")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code valide et réduction calculée"),
            @ApiResponse(responseCode = "404", description = "Code promo introuvable ou invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> validateAndCalculateDiscount(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {
        try {
            if (code == null || code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code promo requis"));
            }
            if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sous-total invalide"));
            }

            Optional<Promotion> promoOpt = promotionService.findValidPromotionByCode(code);
            if (promoOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Code promo introuvable ou expiré"));
            }

            Promotion promo = promoOpt.get();
            BigDecimal discountAmount = promotionService.calculateDiscount(subtotal, promo);
            BigDecimal totalAfterDiscount = subtotal.subtract(discountAmount);
            if (totalAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
                totalAfterDiscount = BigDecimal.ZERO;
            }

            PromotionValidationResponse response = new PromotionValidationResponse(
                    promo.getCode(),
                    discountAmount,
                    subtotal,
                    totalAfterDiscount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/resolve-influencer")
    @Operation(summary = "Résoudre une promotion à partir d'un code influenceur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion résolue", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Promotion.class))),
            @ApiResponse(responseCode = "404", description = "Promotion introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> resolveFromInfluencer(@RequestParam String code) {
        try {
            Optional<Promotion> promo = promotionService.resolveFromInfluencerCode(code);
            return promo.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(java.util.Map.of("error", "Promotion introuvable")));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer une promotion")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion créée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(@Valid @RequestBody PromotionCreateDTO dto) {
        try {
            PromotionResponse response = promotionService.create(dto);
            return ResponseEntity.ok(Map.of("message", "Promotion créée avec succès", "promotion", response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/admin")
    @Operation(summary = "Lister toutes les promotions (admin)", description = "Récupère la liste de toutes les promotions")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(promotionService.findAll());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "Récupérer une promotion par ID (admin)")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion trouvée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return promotionService.findById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(Map.of("error", "Promotion introuvable")));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/admin/{id}")
    @Operation(summary = "Mettre à jour une promotion (admin)")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion mise à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromotionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Promotion introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdatePromotionRequest request) {
        try {
            PromotionResponse response = promotionService.update(id, request);
            return ResponseEntity.ok(Map.of("message", "Promotion mise à jour avec succès", "promotion", response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "Supprimer une promotion (admin)")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion supprimée"),
            @ApiResponse(responseCode = "404", description = "Promotion introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            promotionService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Promotion supprimée avec succès"));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
