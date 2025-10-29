package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import com.pneumaliback.www.dto.PromotionCreateDTO;

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
        return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
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
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion créée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Promotion.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(@RequestBody PromotionCreateDTO dto) {
        try {
            return ResponseEntity.ok(promotionService.create(dto));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

