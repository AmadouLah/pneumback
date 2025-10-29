package com.pneumaliback.www.controller;

import com.pneumaliback.www.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Paiements - Callback", description = "Callbacks des prestataires de paiement")
public class PaymentCallbackController {

    private final PaymentService paymentService;

    @PostMapping("/callback/success")
    @Operation(summary = "Callback succès paiement", description = "Confirme le paiement et la commande via la référence transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Callback traité"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> handleSuccessCallback(@RequestParam("txRef") String transactionReference) {
        try {
            paymentService.confirmSuccessByTransaction(transactionReference);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Requête invalide"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
        }
    }
}

