package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.MessageDTO;
import com.pneumaliback.www.entity.Message;
import com.pneumaliback.www.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Messages", description = "Messagerie entre utilisateurs")
public class MessageController {

    private final MessageService messageService;

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

    @PostMapping
    @Operation(summary = "Envoyer un message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message envoyé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Message.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> envoyer(@RequestBody MessageDTO dto) {
        try {
            return ResponseEntity.ok(messageService.send(dto.authorId(), dto.recipientId(), dto.content()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/inbox/{destinataireId}")
    @Operation(summary = "Boîte de réception")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages récupérés"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> inbox(@PathVariable Long destinataireId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(messageService.inbox(destinataireId, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/outbox/{auteurId}")
    @Operation(summary = "Boîte d'envoi")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages récupérés"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> outbox(@PathVariable Long auteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(messageService.outbox(auteurId, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/conversation")
    @Operation(summary = "Conversation entre deux utilisateurs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation récupérée"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> conversation(@RequestParam Long u1,
            @RequestParam Long u2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(messageService.conversation(u1, u2, pageable));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/conversation/lire")
    @Operation(summary = "Marquer comme lue la conversation reçue d'un interlocuteur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation marquée lue"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> marquerLue(@RequestParam Long destinataireId, @RequestParam Long auteurId) {
        try {
            int updated = messageService.markConversationRead(destinataireId, auteurId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/non-lus")
    @Operation(summary = "Compteur des non-lus par interlocuteur pour un destinataire")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compteurs récupérés"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> nonLus(@RequestParam Long destinataireId) {
        try {
            return ResponseEntity.ok(messageService.unreadByInterlocutor(destinataireId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/threads")
    @Operation(summary = "Liste des threads: dernier message + compteur non lus")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Threads récupérés"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> threads(@RequestParam Long userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            return ResponseEntity.ok(messageService.threadList(userId, q, limit));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

