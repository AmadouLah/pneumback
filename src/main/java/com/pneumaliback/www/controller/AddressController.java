package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.AddAddressRequest;
import com.pneumaliback.www.dto.UpdateAddressRequest;
import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Country;
import com.pneumaliback.www.repository.AddressRepository;
import com.pneumaliback.www.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Adresses", description = "API de gestion des adresses")
@CrossOrigin(origins = "*")
public class AddressController {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private ResponseEntity<?> handleException(Exception e) {
        String msg = e.getMessage();
        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg != null ? msg : "Requête invalide"));
        }
        return ResponseEntity.internalServerError()
                .body(java.util.Map.of("error", "Erreur interne du serveur", "message", msg));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Ajouter une adresse", description = "Permet à un utilisateur d'ajouter une nouvelle adresse")
    public ResponseEntity<?> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddAddressRequest request) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Si cette adresse est définie comme adresse par défaut, désactiver les autres
            if (request.isDefault()) {
                List<Address> userAddresses = addressRepository.findByUser(user);
                userAddresses.forEach(addr -> addr.setDefault(false));
                addressRepository.saveAll(userAddresses);
            }

            Address address = new Address();
            address.setStreet(request.street());
            address.setCity(request.city());
            address.setRegion(request.region());
            address.setCountry(Country.valueOf(request.country()));
            address.setPostalCode(request.postalCode());
            address.setPhoneNumber(request.phoneNumber());
            address.setDefault(request.isDefault());
            address.setUser(user);

            addressRepository.save(address);

            return ResponseEntity.ok(java.util.Map.of("message", "Adresse ajoutée avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de l'adresse pour l'utilisateur {}: {}", userDetails.getUsername(),
                    e.getMessage());
            return handleException(e);
        }
    }

    @GetMapping
    @Operation(summary = "Obtenir les adresses de l'utilisateur", description = "Récupère toutes les adresses de l'utilisateur connecté")
    public ResponseEntity<?> getUserAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<Address> addresses = addressRepository.findByUser(user);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des adresses pour l'utilisateur {}: {}",
                    userDetails.getUsername(), e.getMessage());
            return handleException(e);
        }
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Mettre à jour une adresse", description = "Met à jour uniquement les champs fournis d'une adresse")
    public ResponseEntity<?> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Address address = addressRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Adresse non trouvée"));

            // Vérifier que l'adresse appartient à l'utilisateur
            if (!address.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette adresse");
            }

            // Mettre à jour uniquement les champs fournis
            if (request.street() != null && !request.street().isBlank()) {
                address.setStreet(request.street());
            }
            if (request.city() != null && !request.city().isBlank()) {
                address.setCity(request.city());
            }
            if (request.region() != null && !request.region().isBlank()) {
                address.setRegion(request.region());
            }
            if (request.country() != null && !request.country().isBlank()) {
                address.setCountry(Country.valueOf(request.country()));
            }
            if (request.postalCode() != null) {
                address.setPostalCode(request.postalCode());
            }
            if (request.phoneNumber() != null) {
                address.setPhoneNumber(request.phoneNumber());
            }
            if (request.isDefault() != null) {
                // Si cette adresse devient la par défaut, désactiver les autres
                if (request.isDefault()) {
                    List<Address> userAddresses = addressRepository.findByUser(user);
                    userAddresses.forEach(addr -> addr.setDefault(false));
                    addressRepository.saveAll(userAddresses);
                }
                address.setDefault(request.isDefault());
            }

            addressRepository.save(address);

            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Adresse mise à jour avec succès",
                    "address", address));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'adresse {} pour l'utilisateur {}: {}",
                    id, userDetails.getUsername(), e.getMessage());
            return handleException(e);
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Supprimer une adresse", description = "Supprime une adresse de l'utilisateur")
    public ResponseEntity<?> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Address address = addressRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Adresse non trouvée"));

            // Vérifier que l'adresse appartient à l'utilisateur
            if (!address.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette adresse");
            }

            addressRepository.delete(address);

            return ResponseEntity.ok(java.util.Map.of("message", "Adresse supprimée avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'adresse {} pour l'utilisateur {}: {}",
                    id, userDetails.getUsername(), e.getMessage());
            return handleException(e);
        }
    }
}
