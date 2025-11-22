package com.pneumaliback.www.controller;

import java.util.List;

import com.pneumaliback.www.entity.Delivery;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.DeliveryRepository;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.DeliveryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livreur/deliveries")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('LIVREUR')")
@CrossOrigin(origins = "*")
public class LivreurDeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final DeliveryService deliveryService;

    @GetMapping
    @Operation(summary = "Livraisons assignées au livreur")
    @ApiResponse(responseCode = "200", description = "Livraisons récupérées", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Delivery.class))))
    public ResponseEntity<List<Delivery>> listAssigned(@AuthenticationPrincipal UserDetails principal) {
        User livreur = resolveLivreur(principal);
        return ResponseEntity.ok(deliveryRepository.findByAssignedLivreurOrderByCreatedAtDesc(livreur));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Marquer une livraison comme effectuée")
    public ResponseEntity<Delivery> completeDelivery(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        User livreur = resolveLivreur(principal);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable"));
        if (delivery.getAssignedLivreur() == null || !delivery.getAssignedLivreur().getId().equals(livreur.getId())) {
            throw new IllegalStateException("Cette livraison n'est pas assignée à ce livreur.");
        }
        return ResponseEntity.ok(deliveryService.markDelivered(delivery));
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
