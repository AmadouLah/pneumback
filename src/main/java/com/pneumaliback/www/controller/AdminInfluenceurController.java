package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.CreateInfluenceurRequest;
import com.pneumaliback.www.dto.InfluenceurResponse;
import com.pneumaliback.www.service.InfluenceurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/influenceurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des influenceurs", description = "API de gestion des influenceurs - Accès admin uniquement")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminInfluenceurController {

    private final InfluenceurService influenceurService;

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping
    @Operation(summary = "Liste des influenceurs", description = "Récupère la liste de tous les influenceurs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InfluenceurResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAllInfluenceurs() {
        try {
            log.info("Récupération de la liste des influenceurs par l'admin");
            return ResponseEntity.ok(influenceurService.findAll());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer un influenceur", description = "Crée un nouvel influenceur et envoie un email de bienvenue")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Influenceur créé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InfluenceurResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createInfluenceur(@Valid @RequestBody CreateInfluenceurRequest request) {
        try {
            log.info("Création d'un nouvel influenceur: {}", request.email());
            InfluenceurResponse influenceur = influenceurService.createInfluenceur(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Influenceur créé avec succès. Un email de bienvenue a été envoyé.",
                    "influenceur", influenceur));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
