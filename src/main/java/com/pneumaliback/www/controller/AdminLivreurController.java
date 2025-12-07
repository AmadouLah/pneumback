package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.CreateLivreurRequest;
import com.pneumaliback.www.dto.LivreurResponse;
import com.pneumaliback.www.dto.UpdateLivreurRequest;
import com.pneumaliback.www.service.LivreurService;
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
@RequestMapping("/api/admin/livreurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des livreurs", description = "API de gestion des livreurs - Accès admin uniquement")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
@CrossOrigin(origins = "*")
public class AdminLivreurController {

    private final LivreurService livreurService;

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping
    @Operation(summary = "Liste des livreurs", description = "Récupère la liste de tous les livreurs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LivreurResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAllLivreurs() {
        try {
            log.info("Récupération de la liste des livreurs par l'admin");
            return ResponseEntity.ok(livreurService.findAll());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer un livreur", description = "Crée un nouvel livreur et envoie un email de bienvenue")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livreur créé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LivreurResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createLivreur(@Valid @RequestBody CreateLivreurRequest request) {
        try {
            log.info("Création d'un nouveau livreur: {}", request.email());
            LivreurResponse livreur = livreurService.createLivreur(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Livreur créé avec succès. Un email de bienvenue a été envoyé.",
                    "livreur", livreur));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un livreur", description = "Met à jour les informations d'un livreur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livreur mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LivreurResponse.class))),
            @ApiResponse(responseCode = "404", description = "Livreur introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateLivreur(@PathVariable Long id,
            @Valid @RequestBody UpdateLivreurRequest request) {
        try {
            LivreurResponse response = livreurService.updateLivreur(id, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Livreur mis à jour avec succès",
                    "livreur", response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Activer/Désactiver un livreur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LivreurResponse.class))),
            @ApiResponse(responseCode = "404", description = "Livreur introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam boolean active) {
        try {
            LivreurResponse response = livreurService.toggleActive(id, active);
            String message = active
                    ? "Livreur activé avec succès."
                    : "Livreur désactivé.";
            return ResponseEntity.ok(Map.of("message", message, "livreur", response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "Supprimer un livreur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livreur supprimé"),
            @ApiResponse(responseCode = "404", description = "Livreur introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            livreurService.deleteLivreur(id);
            return ResponseEntity.ok(Map.of("message", "Livreur supprimé avec succès."));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
