package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Favori;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.FavoriService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favoris")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Favoris", description = "Gestion des favoris utilisateur")
public class FavoriController {

    private final FavoriService favoriService;
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
    @Operation(summary = "Lister favoris d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Favori.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> list(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(favoriService.listByUser(getUser(userId)));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Ajouter un favori")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favori ajouté", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Favori.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou produit non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> add(@RequestParam Long userId, @RequestParam Long productId) {
        try {
            return ResponseEntity.ok(favoriService.add(getUser(userId), productId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping
    @Operation(summary = "Supprimer un favori")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Favori supprimé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou favori non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> remove(@RequestParam Long userId, @RequestParam Long productId) {
        try {
            favoriService.remove(getUser(userId), productId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

