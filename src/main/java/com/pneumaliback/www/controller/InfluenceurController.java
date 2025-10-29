package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/influenceur")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Influenceur", description = "API des fonctionnalités influenceur")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('INFLUENCEUR')")
@CrossOrigin(origins = "*")
public class InfluenceurController {

    private final UserRepository userRepository;

    private ResponseEntity<Map<String, String>> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping("/profile")
    @Operation(summary = "Profil influenceur", description = "Récupère le profil de l'influenceur connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil récupéré", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getInfluenceurProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            log.info("Récupération du profil de l'influenceur: {}", email);
            Optional<User> user = userRepository.findByEmail(email);
            return user.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé")));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour le profil de l'influenceur connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateInfluenceurProfile(@RequestBody User updatedProfile) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            log.info("Mise à jour du profil de l'influenceur: {}", email);
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setFirstName(updatedProfile.getFirstName());
                user.setLastName(updatedProfile.getLastName());
                user.setPhoneNumber(updatedProfile.getPhoneNumber());
                User savedUser = userRepository.save(user);
                log.info("Profil de l'influenceur {} mis à jour avec succès", email);
                return ResponseEntity.ok(savedUser);
            }
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques influenceur", description = "Récupère les statistiques de l'influenceur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InfluenceurStats.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getInfluenceurStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            log.info("Récupération des statistiques de l'influenceur: {}", email);
            InfluenceurStats stats = new InfluenceurStats(
                    email,
                    0L,
                    0L,
                    0L,
                    0L
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    public record InfluenceurStats(
            String email,
            long followers,
            long posts,
            long likes,
            long comments) {
    }
}
