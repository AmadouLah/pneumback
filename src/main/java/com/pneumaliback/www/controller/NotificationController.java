package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.NotificationRechercheDTO;
import com.pneumaliback.www.entity.Notification;
import com.pneumaliback.www.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

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

    @GetMapping("/{userId}")
    @Operation(summary = "Liste des notifications d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> list(@PathVariable Long userId, Pageable pageable) {
        try {
            return ResponseEntity.ok(notificationService.list(userId, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{userId}/non-lues")
    @Operation(summary = "Liste des notifications non lues d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> nonLues(@PathVariable Long userId, Pageable pageable) {
        try {
            return ResponseEntity.ok(notificationService.unread(userId, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/rechercher")
    @Operation(summary = "Recherche avancée de notifications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Résultats récupérés"),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> rechercher(@RequestBody NotificationRechercheDTO criteres,
            Pageable pageable) {
        try {
            return ResponseEntity.ok(notificationService.search(criteres, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/lu")
    @Operation(summary = "Marquer une notification comme lue")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marquée lue"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Notification ou utilisateur introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> marquerLu(@PathVariable Long id, @RequestParam Long userId) {
        try {
            notificationService.markAsRead(id, userId);
            notificationService.sendUnreadCount(notificationService.findUserById(userId));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{userId}/marquer-toutes-lues")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications marquées lues"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> marquerToutesCommeLues(@PathVariable Long userId) {
        try {
            int count = notificationService.markAllAsRead(userId);
            notificationService.sendUnreadCount(notificationService.findUserById(userId));
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{userId}/count/non-lues")
    @Operation(summary = "Compter les notifications non lues")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compteur récupéré"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> countNonLues(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(notificationService.countUnread(userId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{userId}/count/non-lues/{type}")
    @Operation(summary = "Compter les notifications non lues par type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compteur récupéré"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> countNonLuesParType(@PathVariable Long userId, @PathVariable String type) {
        try {
            return ResponseEntity.ok(notificationService.countUnreadByType(userId, type));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{userId}/statistiques")
    @Operation(summary = "Statistiques des notifications par type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> statistiquesParType(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(notificationService.statisticsByType(userId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/{userId}/nettoyer")
    @Operation(summary = "Nettoyer les anciennes notifications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nettoyage effectué"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> nettoyerAnciennes(@PathVariable Long userId,
            @RequestParam(defaultValue = "30") int joursRetention) {
        try {
            int count = notificationService.cleanupOld(userId, joursRetention);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    // ===== ENDPOINTS WEBSOCKET =====

    /**
     * Marque une notification comme lue via WebSocket
     * Endpoint: /app/notifications/{userId}/marquer-lue
     */
    @MessageMapping("/notifications/{userId}/marquer-lue")
    @PreAuthorize("isAuthenticated()")
    public void marquerLuWebSocket(@DestinationVariable Long userId, @Payload Map<String, Object> payload) {
        try {
            Long notificationId = Long.valueOf(payload.get("notificationId").toString());
            log.info("WebSocket: Marquer notification {} comme lue pour utilisateur {}", notificationId, userId);

            notificationService.markAsRead(notificationId, userId);

            // Envoyer le nouveau count via WebSocket
            notificationService.sendUnreadCount(
                    notificationService.findUserById(userId));
        } catch (Exception e) {
            log.error("Erreur lors du marquage de notification via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Marque toutes les notifications comme lues via WebSocket
     * Endpoint: /app/notifications/{userId}/marquer-toutes-lues
     */
    @MessageMapping("/notifications/{userId}/marquer-toutes-lues")
    @PreAuthorize("isAuthenticated()")
    public void marquerToutesLuesWebSocket(@DestinationVariable Long userId) {
        try {
            log.info("WebSocket: Marquer toutes les notifications comme lues pour utilisateur {}", userId);

            notificationService.markAllAsRead(userId);

            // Envoyer le nouveau count via WebSocket (0 car toutes marquées comme lues)
            notificationService.sendUnreadCount(
                    notificationService.findUserById(userId));
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Demande le count actuel des notifications non lues via WebSocket
     * Endpoint: /app/notifications/{userId}/demander-count
     */
    @MessageMapping("/notifications/{userId}/demander-count")
    @PreAuthorize("isAuthenticated()")
    public void demanderCountWebSocket(@DestinationVariable Long userId) {
        try {
            log.debug("WebSocket: Demande count notifications non lues pour utilisateur {}", userId);

            // Envoyer le count actuel via WebSocket
            notificationService.sendUnreadCount(
                    notificationService.findUserById(userId));
        } catch (Exception e) {
            log.error("Erreur lors de la demande de count via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Souscription aux notifications d'un utilisateur
     * Endpoint: /app/notifications/{userId}/souscrire
     */
    @MessageMapping("/notifications/{userId}/souscrire")
    @PreAuthorize("isAuthenticated()")
    public void souscrireNotifications(@DestinationVariable Long userId) {
        try {
            log.info("WebSocket: Souscription aux notifications pour utilisateur {}", userId);

            // Envoyer immédiatement le count actuel
            notificationService.sendUnreadCount(
                    notificationService.findUserById(userId));
        } catch (Exception e) {
            log.error("Erreur lors de la souscription aux notifications via WebSocket: {}", e.getMessage());
        }
    }

}
