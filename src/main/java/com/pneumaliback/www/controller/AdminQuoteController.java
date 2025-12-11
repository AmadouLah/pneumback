package com.pneumaliback.www.controller;

import java.util.List;

import com.pneumaliback.www.dto.quote.AssignLivreurRequest;
import com.pneumaliback.www.dto.quote.QuoteAdminUpdateRequest;
import com.pneumaliback.www.dto.quote.QuoteResponse;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.QuoteStatus;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.repository.AddressRepository;
import com.pneumaliback.www.service.QuotePdfService;
import com.pneumaliback.www.service.QuoteRequestService;
import com.pneumaliback.www.service.QuoteRequestService.QuoteAdminItem;
import com.pneumaliback.www.service.QuoteRequestService.QuoteAdminUpdate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/quotes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
@CrossOrigin(origins = "*")
public class AdminQuoteController {

    private final QuoteRequestService quoteRequestService;
    private final QuotePdfService quotePdfService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @GetMapping
    @Operation(summary = "Lister les demandes de devis (admin)")
    @ApiResponse(responseCode = "200", description = "Liste des devis", content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuoteResponse.class))))
    public ResponseEntity<List<QuoteResponse>> listQuotes(
            @RequestParam(name = "status", required = false) List<QuoteStatus> statuses) {
        List<QuoteResponse> responses = quoteRequestService.listForAdmin(statuses).stream()
                .map(QuoteResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une demande de devis (admin)")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable Long id) {
        QuoteRequest request = quoteRequestService.getById(id);
        return ResponseEntity.ok(QuoteResponse.from(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour le contenu d'un devis avant envoi")
    public ResponseEntity<QuoteResponse> updateQuote(
            @PathVariable Long id,
            @Valid @RequestBody QuoteAdminUpdateRequest payload) {
        QuoteRequest updated = quoteRequestService.updateAdminQuote(id, toServiceUpdate(payload));
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Générer et envoyer le devis au client")
    public ResponseEntity<QuoteResponse> generateAndSend(
            @PathVariable Long id,
            @Valid @RequestBody QuoteAdminUpdateRequest payload,
            @RequestParam(name = "quoteUrl", required = false) String frontendQuoteUrl) {
        QuoteRequest updated = quoteRequestService.generateAndSendQuote(id, toServiceUpdate(payload), frontendQuoteUrl);
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "Générer un aperçu PDF du devis")
    public ResponseEntity<QuoteResponse> generatePreview(@PathVariable Long id) {
        QuoteRequest updated = quoteRequestService.renderQuotePreview(id);
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    @GetMapping("/{id}/preview-pdf")
    @Operation(summary = "Télécharger l'aperçu PDF du devis")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<byte[]> getPreviewPdf(@PathVariable Long id) {
        try {
            QuoteRequest request = quoteRequestService.getById(id);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }

            User emitter = resolveCurrentAdmin();
            if (emitter != null) {
                quoteRequestService.loadUserAddresses(emitter);
            }

            if (request.getUser() != null) {
                quoteRequestService.loadUserAddresses(request.getUser());
            }

            byte[] pdf = quotePdfService.generateQuote(request, emitter);

            if (pdf == null || pdf.length == 0) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "devis-" + request.getRequestNumber() + ".pdf");
            headers.setContentLength(pdf.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private User resolveCurrentAdmin() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmailWithAddresses(authentication.getName()).orElse(null);
    }

    private void loadUserAddresses(User user) {
        if (user != null && user.getId() != null) {
            try {
                java.util.List<com.pneumaliback.www.entity.Address> addresses = addressRepository.findByUser(user);
                if (addresses != null && !addresses.isEmpty()) {
                    user.getAddresses().clear();
                    user.getAddresses().addAll(addresses);
                }
            } catch (Exception e) {
                // Ignore si les adresses ne peuvent pas être chargées
            }
        }
    }

    @PostMapping("/{id}/assign-livreur")
    @Operation(summary = "Assigner un livreur pour la livraison")
    public ResponseEntity<QuoteResponse> assignLivreur(
            @PathVariable Long id,
            @Valid @RequestBody AssignLivreurRequest payload) {
        User livreur = userRepository.findById(payload.livreurId())
                .orElseThrow(() -> new IllegalArgumentException("Livreur introuvable"));
        if (livreur.getRole() != Role.LIVREUR) {
            throw new IllegalArgumentException("L'utilisateur sélectionné n'est pas un livreur.");
        }
        QuoteRequest updated = quoteRequestService.assignLivreur(id, livreur, payload.deliveryDetails());
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    @PostMapping("/regenerate-all-pdfs")
    @Operation(summary = "Regénérer tous les PDFs existants avec le nouveau format", 
               description = "Remplace tous les anciens PDFs dans Supabase par les nouveaux (sans le message du client)")
    public ResponseEntity<java.util.Map<String, Object>> regenerateAllPdfs() {
        try {
            int regeneratedCount = quoteRequestService.regenerateAllPdfs();
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Migration des PDFs terminée",
                    "regeneratedCount", regeneratedCount));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                            "success", false,
                            "message", "Erreur lors de la migration: " + e.getMessage()));
        }
    }

    private QuoteAdminUpdate toServiceUpdate(QuoteAdminUpdateRequest payload) {
        List<QuoteAdminItem> items = payload.items() != null
                ? payload.items().stream()
                        .map(i -> new QuoteAdminItem(
                                i.productId(),
                                i.productName(),
                                i.brand(),
                                i.width(),
                                i.profile(),
                                i.diameter(),
                                i.quantity(),
                                i.unitPrice()))
                        .toList()
                : null;
        return new QuoteAdminUpdate(
                items,
                payload.discountTotal(),
                payload.totalQuoted(),
                payload.validUntil(),
                payload.adminNotes(),
                payload.deliveryDetails());
    }
}
