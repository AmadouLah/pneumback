package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.Delivery;
import com.pneumaliback.www.enums.DeliveryStatus;
import com.pneumaliback.www.repository.DeliveryRepository;
import com.pneumaliback.www.repository.AddressRepository;
import com.pneumaliback.www.repository.OrderRepository;
import com.pneumaliback.www.service.DeliveryService;
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

import java.math.BigDecimal;
import com.pneumaliback.www.dto.DeliveryCreateDTO;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Livraisons", description = "Gestion des livraisons")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryRepository deliveryRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;

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

    @GetMapping("/quote")
    @Operation(summary = "Devis des frais de livraison pour une zone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devis calculé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BigDecimal.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> quote(@RequestParam String zone) {
        try {
            return ResponseEntity.ok(deliveryService.quoteShippingFee(zone));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping
    @Operation(summary = "Créer une livraison pour une commande")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livraison créée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Delivery.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Commande ou adresse introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> create(@RequestBody DeliveryCreateDTO dto) {
        try {
            if (dto == null || dto.orderId() == null || dto.addressId() == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Paramètres invalides"));
            }
            var order = orderRepository.findById(dto.orderId())
                    .orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
            var address = addressRepository.findById(dto.addressId())
                    .orElseThrow(() -> new IllegalArgumentException("Adresse introuvable"));
            BigDecimal fee = dto.shippingFee() != null ? dto.shippingFee() : deliveryService.quoteShippingFee(dto.zone());
            Delivery d = deliveryService.attachDelivery(order, address, dto.zone(), fee);
            return ResponseEntity.ok(d);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{deliveryId}/status")
    @Operation(summary = "Mettre à jour le statut d'une livraison")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Delivery.class))),
            @ApiResponse(responseCode = "404", description = "Livraison introuvable", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateStatus(@PathVariable Long deliveryId, @RequestParam DeliveryStatus status) {
        try {
            Delivery d = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable"));
            return ResponseEntity.ok(deliveryService.updateStatus(d, status));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

