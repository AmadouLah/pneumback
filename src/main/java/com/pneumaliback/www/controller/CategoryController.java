package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.service.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Catégories", description = "Gestion des catégories")
public class CategoryController {

    private final CategoryService categoryService;

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

    @GetMapping("/active")
    @Operation(summary = "Lister catégories actives")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listActive() {
        try {
            return ResponseEntity.ok(categoryService.listActive());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer une catégorie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catégorie créée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(@RequestBody Category c) {
        try {
            return ResponseEntity.ok(categoryService.create(c));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une catégorie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catégorie mise à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Category payload) {
        try {
            return ResponseEntity.ok(categoryService.update(id, payload));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "Activer/Désactiver une catégorie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "404", description = "Catégorie non trouvée", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> toggle(@PathVariable Long id, @RequestParam boolean active) {
        try {
            return ResponseEntity.ok(categoryService.toggleActive(id, active));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

