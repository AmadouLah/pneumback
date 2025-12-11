package com.pneumaliback.www.controller;

import java.util.List;

import com.pneumaliback.www.dto.quote.CreateQuoteRequestPayload;
import com.pneumaliback.www.dto.quote.QuoteResponse;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.QuoteRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class QuoteRequestController {

    private final QuoteRequestService quoteRequestService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    @Operation(summary = "Créer une demande de devis à partir du panier")
    @ApiResponse(responseCode = "200", description = "Demande créée", content = @Content(schema = @Schema(implementation = QuoteResponse.class)))
    public ResponseEntity<QuoteResponse> createQuoteFromCart(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateQuoteRequestPayload payload) {
        User user = resolveUser(principal);
        QuoteRequest request = quoteRequestService.createFromPayload(user, payload);
        return ResponseEntity.ok(QuoteResponse.from(request));
    }

    @GetMapping
    @Operation(summary = "Lister les devis du client connecté")
    @ApiResponse(responseCode = "200", description = "Liste des devis", content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuoteResponse.class))))
    public ResponseEntity<List<QuoteResponse>> listQuotes(
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        List<QuoteResponse> responses = quoteRequestService.listForUser(user).stream()
                .map(QuoteResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un devis du client connecté")
    @ApiResponse(responseCode = "200", description = "Devis trouvé", content = @Content(schema = @Schema(implementation = QuoteResponse.class)))
    public ResponseEntity<QuoteResponse> getQuote(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        User user = resolveUser(principal);
        QuoteRequest request = quoteRequestService.getById(id);
        if (!request.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Devis introuvable");
        }
        return ResponseEntity.ok(QuoteResponse.from(request));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Valider un devis (signature électronique)")
    @ApiResponse(responseCode = "200", description = "Devis validé", content = @Content(schema = @Schema(implementation = QuoteResponse.class)))
    public ResponseEntity<QuoteResponse> validateQuote(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody(required = false) ValidateQuotePayload payload,
            HttpServletRequest httpRequest) {
        User user = resolveUser(principal);
        QuoteRequest request = quoteRequestService.getById(id);
        if (!request.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Devis introuvable");
        }
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpRequest.getRemoteAddr();
        }
        String deviceInfo = payload != null ? payload.deviceInfo() : null;
        if (deviceInfo == null || deviceInfo.isBlank()) {
            deviceInfo = httpRequest.getHeader("User-Agent");
        }
        QuoteRequest updated = quoteRequestService.validateByClient(
                request.getId(), 
                clientIp, 
                deviceInfo,
                payload != null ? payload.requestedDeliveryDate() : null);
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    public record ValidateQuotePayload(java.time.LocalDate requestedDeliveryDate, String deviceInfo) {}

    @PostMapping("/{id}/confirm-delivery")
    @Operation(summary = "Confirmer la livraison par le client")
    @ApiResponse(responseCode = "200", description = "Livraison confirmée", content = @Content(schema = @Schema(implementation = QuoteResponse.class)))
    public ResponseEntity<QuoteResponse> confirmDelivery(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        User user = resolveUser(principal);
        QuoteRequest request = quoteRequestService.getById(id);
        if (!request.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Devis introuvable");
        }
        QuoteRequest updated = quoteRequestService.confirmDeliveryByClient(request.getId(), user);
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    private User resolveUser(UserDetails principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }
        return userRepository.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }
}
