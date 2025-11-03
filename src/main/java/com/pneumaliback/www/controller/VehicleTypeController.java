package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.VehicleType;
import com.pneumaliback.www.service.VehicleTypeService;
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
@RequestMapping("/api/vehicle-types")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Types de véhicules", description = "Gestion des types de véhicules")
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

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
    @Operation(summary = "Lister tous les types de véhicules (admin)", description = "Liste tous les types, y compris inactifs")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleType.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listAll() {
        try {
            return ResponseEntity.ok(vehicleTypeService.listAll());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Lister types de véhicules actifs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleType.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listActive() {
        try {
            return ResponseEntity.ok(vehicleTypeService.listActive());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Lister types de véhicules par catégorie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleType.class))),
            @ApiResponse(responseCode = "404", description = "Catégorie introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listByCategory(@PathVariable Long categoryId) {
        try {
            return ResponseEntity.ok(vehicleTypeService.listByCategory(categoryId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer un type de véhicule")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Type créé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleType.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(@RequestBody VehicleType payload) {
        try {
            return ResponseEntity.ok(vehicleTypeService.create(payload));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un type de véhicule")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Type mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleType.class))),
            @ApiResponse(responseCode = "404", description = "Type non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VehicleType payload) {
        try {
            return ResponseEntity.ok(vehicleTypeService.update(id, payload));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "Activer/Désactiver un type de véhicule")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VehicleType.class))),
            @ApiResponse(responseCode = "404", description = "Type non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> toggle(@PathVariable Long id, @RequestParam boolean active) {
        try {
            return ResponseEntity.ok(vehicleTypeService.toggleActive(id, active));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un type de véhicule")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Type supprimé"),
            @ApiResponse(responseCode = "404", description = "Type non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Impossible de supprimer (utilisé par des produits)", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            vehicleTypeService.delete(id);
            return ResponseEntity.ok(java.util.Map.of("message", "Type de véhicule supprimé avec succès"));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
