package com.pneumaliback.www.controller;

import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.entity.Commission;
import com.pneumaliback.www.enums.CommissionStatus;
import com.pneumaliback.www.repository.OrderRepository;
import com.pneumaliback.www.repository.CommissionRepository;
import com.pneumaliback.www.service.OrderService;
import com.pneumaliback.www.service.CommissionService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administration", description = "API d'administration - Accès admin uniquement")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CommissionRepository commissionRepository;
    private final OrderService orderService;
    private final CommissionService commissionService;

    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String msg = e.getMessage() != null ? e.getMessage() : "Requête invalide";
            if (msg.toLowerCase().contains("introuvable") || msg.toLowerCase().contains("non trouv")) {
                return ResponseEntity.status(404).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
        return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne du serveur", "message", e.getMessage()));
    }

    @GetMapping("/users")
    @Operation(summary = "Liste des utilisateurs", description = "Récupère la liste de tous les utilisateurs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAllUsers() {
        try {
            log.info("Récupération de la liste des utilisateurs par l'admin");
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Détails d'un utilisateur", description = "Récupère les détails d'un utilisateur spécifique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            log.info("Récupération des détails de l'utilisateur ID: {}", id);
            Optional<User> user = userRepository.findById(id);
            return user.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé")));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Utilisateurs par rôle", description = "Récupère tous les utilisateurs d'un rôle spécifique")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getUsersByRole(@PathVariable Role role) {
        try {
            log.info("Récupération des utilisateurs avec le rôle: {}", role);
            List<User> users = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == role)
                    .toList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Modifier le rôle d'un utilisateur", description = "Change le rôle d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rôle modifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestParam Role newRole) {
        try {
            log.info("Modification du rôle de l'utilisateur ID: {} vers: {}", id, newRole);
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setRole(newRole);
                User savedUser = userRepository.save(user);
                log.info("Rôle de l'utilisateur {} modifié vers: {}", user.getEmail(), newRole);
                return ResponseEntity.ok(savedUser);
            }
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Modifier le statut d'un utilisateur", description = "Active ou désactive un compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        try {
            log.info("Modification du statut de l'utilisateur ID: {} vers: {}", id, enabled);
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setEnabled(enabled);
                User savedUser = userRepository.save(user);
                log.info("Statut de l'utilisateur {} modifié vers: {}", user.getEmail(), enabled);
                return ResponseEntity.ok(savedUser);
            }
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/users/{id}/lock")
    @Operation(summary = "Verrouiller/Déverrouiller un compte", description = "Verrouille ou déverrouille un compte utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut de verrouillage modifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> toggleUserLock(@PathVariable Long id) {
        try {
            log.info("Modification du verrouillage de l'utilisateur ID: {}", id);
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean newLockStatus = !user.isAccountNonLocked();
                user.setAccountNonLocked(newLockStatus);
                if (newLockStatus) {
                    user.setFailedAttempts(0);
                    user.setLockTime(null);
                    log.info("Compte de l'utilisateur {} déverrouillé", user.getEmail());
                } else {
                    log.info("Compte de l'utilisateur {} verrouillé", user.getEmail());
                }
                User savedUser = userRepository.save(user);
                return ResponseEntity.ok(savedUser);
            }
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques des utilisateurs", description = "Récupère les statistiques des utilisateurs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getUserStats() {
        try {
            log.info("Récupération des statistiques des utilisateurs");
            List<User> allUsers = userRepository.findAll();
            UserStats stats = new UserStats(
                    allUsers.size(),
                    allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count(),
                    allUsers.stream().filter(u -> u.getRole() == Role.CLIENT).count(),
                    allUsers.stream().filter(u -> u.getRole() == Role.INFLUENCEUR).count(),
                    allUsers.stream().filter(User::isEnabled).count(),
                    allUsers.stream().filter(u -> !u.isAccountNonLocked()).count());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    public record UserStats(
            long totalUsers,
            long adminCount,
            long clientCount,
            long influenceurCount,
            long activeUsers,
            long lockedUsers) {
    }

    @PutMapping("/orders/{orderId}/confirm")
    @Operation(summary = "Confirmer une commande")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commande confirmée"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) {
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Commande non trouvée"));
            Order order = opt.get();
            orderService.confirm(order);
            Order saved = orderRepository.save(order);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/commissions")
    @Operation(summary = "Liste des commissions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listAllCommissions() {
        try {
            return ResponseEntity.ok(commissionRepository.findAll());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/commissions/influenceur/{influenceurId}")
    @Operation(summary = "Commissions par influenceur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> listCommissionsByInfluenceur(@PathVariable Long influenceurId) {
        try {
            return ResponseEntity.ok(commissionRepository.findByInfluenceurId(influenceurId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/commissions/influenceur/{influenceurId}/balance")
    @Operation(summary = "Solde commissions influenceur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solde récupéré"),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getInfluenceurBalance(@PathVariable Long influenceurId) {
        try {
            BigDecimal total = commissionRepository.sumByInfluenceur(influenceurId);
            BigDecimal paid = commissionRepository.sumByInfluenceurAndStatus(influenceurId, CommissionStatus.PAID);
            BigDecimal pending = commissionRepository.sumByInfluenceurAndStatus(influenceurId, CommissionStatus.PENDING);
            return ResponseEntity.ok(new BalanceDTO(total, paid, pending));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/commissions/{commissionId}/pay")
    @Operation(summary = "Marquer une commission comme payée")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commission payée"),
            @ApiResponse(responseCode = "404", description = "Commission non trouvée", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> payCommission(@PathVariable Long commissionId) {
        try {
            Optional<Commission> opt = commissionRepository.findById(commissionId);
            if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Commission non trouvée"));
            Commission c = opt.get();
            commissionService.markPaid(c);
            return ResponseEntity.ok(c);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    public record BalanceDTO(BigDecimal total, BigDecimal paid, BigDecimal pending) {}
}
