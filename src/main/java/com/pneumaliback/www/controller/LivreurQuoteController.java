package com.pneumaliback.www.controller;

import java.util.List;

import com.pneumaliback.www.dto.quote.MarkClientAbsentPayload;
import com.pneumaliback.www.dto.quote.MarkDeliveryPayload;
import com.pneumaliback.www.dto.quote.QuoteResponse;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.QuoteRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livreur/quotes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('LIVREUR')")
@CrossOrigin(origins = "*")
public class LivreurQuoteController {

    private final QuoteRequestService quoteRequestService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Devis assignés au livreur")
    @ApiResponse(responseCode = "200", description = "Liste des devis", content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuoteResponse.class))))
    public ResponseEntity<List<QuoteResponse>> listAssigned(
            @AuthenticationPrincipal UserDetails principal) {
        User livreur = resolveLivreur(principal);
        List<QuoteResponse> responses = quoteRequestService.listForLivreur(livreur).stream()
                .map(QuoteResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Confirmer la livraison d'un devis avec preuves")
    public ResponseEntity<QuoteResponse> markDelivered(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody MarkDeliveryPayload payload) {
        User livreur = resolveLivreur(principal);
        QuoteRequest updated = quoteRequestService.markDelivered(
                id, livreur, payload.latitude(), payload.longitude(),
                payload.photoBase64(), payload.signatureData(), payload.deliveryNotes());
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    @PostMapping("/{id}/client-absent")
    @Operation(summary = "Marquer un devis comme client absent")
    public ResponseEntity<QuoteResponse> markClientAbsent(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody MarkClientAbsentPayload payload) {
        User livreur = resolveLivreur(principal);
        QuoteRequest updated = quoteRequestService.markClientAbsent(id, livreur, payload.photoBase64(), payload.notes());
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    private User resolveLivreur(UserDetails principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }
        User livreur = userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        if (livreur.getRole() != Role.LIVREUR) {
            throw new IllegalStateException("Accès réservé aux livreurs");
        }
        return livreur;
    }
}
