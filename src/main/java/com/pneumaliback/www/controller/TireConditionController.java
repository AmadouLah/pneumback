package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.TireCondition;
import com.pneumaliback.www.service.TireConditionService;
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

@RestController
@RequestMapping("/api/tire-conditions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "États de pneus", description = "Gestion des états de pneus")
public class TireConditionController {

    private final TireConditionService tireConditionService;

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

    @GetMapping
    @Operation(summary = "Lister tous les états de pneus (admin)", description = "Liste tous les états, y compris inactifs")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TireCondition.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listAll() {
        try {
            return ResponseEntity.ok(tireConditionService.listAll());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Lister états de pneus actifs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TireCondition.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listActive() {
        try {
            return ResponseEntity.ok(tireConditionService.listActive());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer un état de pneu")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "État créé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TireCondition.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(@RequestBody TireCondition payload) {
        try {
            return ResponseEntity.ok(tireConditionService.create(payload));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un état de pneu")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "État mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TireCondition.class))),
            @ApiResponse(responseCode = "404", description = "État non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TireCondition payload) {
        try {
            return ResponseEntity.ok(tireConditionService.update(id, payload));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "Activer/Désactiver un état de pneu")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TireCondition.class))),
            @ApiResponse(responseCode = "404", description = "État non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> toggle(@PathVariable Long id, @RequestParam boolean active) {
        try {
            return ResponseEntity.ok(tireConditionService.toggleActive(id, active));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un état de pneu")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "État supprimé"),
            @ApiResponse(responseCode = "404", description = "État non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Impossible de supprimer (utilisé par des produits)", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            tireConditionService.delete(id);
            return ResponseEntity.ok(java.util.Map.of("message", "État de pneu supprimé avec succès"));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
