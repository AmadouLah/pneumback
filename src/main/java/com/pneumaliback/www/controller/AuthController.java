package com.pneumaliback.www.controller;

import com.pneumaliback.www.dto.AuthResponse;
import com.pneumaliback.www.dto.LoginRequest;
import com.pneumaliback.www.dto.RegisterRequest;
import com.pneumaliback.www.dto.MessageResponse;
import com.pneumaliback.www.dto.ResendVerificationRequest;
import com.pneumaliback.www.dto.ForgotPasswordRequest;
import com.pneumaliback.www.dto.ResetPasswordRequest;
import com.pneumaliback.www.dto.RefreshTokenRequest;
import com.pneumaliback.www.dto.VerificationRequest;
import com.pneumaliback.www.dto.VerifyCodeRequest;
import com.pneumaliback.www.dto.StartLoginResponse;
import com.pneumaliback.www.service.AuthService;
import com.pneumaliback.www.exception.CodeVerificationRequiredException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.StaleStateException;
import jakarta.persistence.OptimisticLockException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentification", description = "API d'authentification et d'inscription")
public class AuthController {

    private final AuthService authService;

    private ResponseEntity<?> handleException(Exception e) {
        String msg = e.getMessage();

        // Gestion spécifique des erreurs de concurrence
        if (e instanceof OptimisticLockException || e instanceof StaleStateException) {
            log.warn("Conflit de concurrence détecté: {}", msg);
            return ResponseEntity.status(409)
                    .body(java.util.Map.of(
                            "error", "Opération en cours ailleurs",
                            "message", "Veuillez réessayer dans quelques secondes"));
        }

        if (e.getCause() instanceof OptimisticLockException || e.getCause() instanceof StaleStateException) {
            log.warn("Conflit de concurrence détecté (cause): {}", msg);
            return ResponseEntity.status(409)
                    .body(java.util.Map.of(
                            "error", "Opération en cours ailleurs",
                            "message", "Veuillez réessayer dans quelques secondes"));
        }

        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg != null ? msg : "Requête invalide"));
        }

        log.error("Erreur serveur: ", e);
        return ResponseEntity.internalServerError()
                .body(java.util.Map.of("error", "Erreur interne du serveur", "message",
                        msg != null ? msg : "Une erreur est survenue"));
    }

    @PostMapping("/start")
    @Operation(summary = "Démarrer le login", description = "Si ADMIN connu: retour mode mot de passe; sinon: envoi code et retour mode email-code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mode de connexion retourné", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StartLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> start(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            StartLoginResponse response = authService.startLogin(request.email());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur", description = "Permet à un utilisateur de s'inscrire")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscription réussie", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Tentative d'inscription pour l'email: {}", request.email());
            MessageResponse response = authService.register(request);
            log.info("Inscription réussie pour l'email: {}", request.email());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/forgot-password/request")
    @Operation(summary = "Demander réinitialisation", description = "Envoie un code de réinitialisation (cooldown 20s)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Demande envoyée", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("Demande de réinitialisation pour l'email: {}", request.email());
            MessageResponse response = authService.requestPasswordReset(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/forgot-password/confirm")
    @Operation(summary = "Confirmer réinitialisation", description = "Valide le code et met à jour le mot de passe")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            log.info("Confirmation de réinitialisation pour l'email: {}", request.email());
            MessageResponse response = authService.confirmPasswordReset(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/verify")
    @Operation(summary = "Vérification du compte", description = "Active le compte avec le code reçu par email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compte vérifié", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> verify(@Valid @RequestBody VerificationRequest request) {
        try {
            log.info("Vérification de compte pour l'email: {}", request.email());
            AuthResponse response = authService.verifyEmail(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/resend")
    @Operation(summary = "Renvoi du code", description = "Renvoyer un nouveau code. Limites: cooldown 20s, jusqu'à 3 renvois après l'envoi initial, réinitialisation après expiration du code (2 min)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code renvoyé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> resend(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            log.info("Renvoi de code pour l'email: {}", request.email());
            MessageResponse response = authService.resendVerificationCode(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/magic/start")
    @Operation(summary = "Démarrer connexion par email", description = "Envoie un code de vérification. Limites: cooldown 20s, jusqu'à 3 renvois après l'envoi initial, réinitialisation après expiration du code (2 min)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code envoyé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> magicStart(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            MessageResponse response = authService.magicStart(request.email());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/magic/verify")
    @Operation(summary = "Vérifier le code et connecter", description = "Valide le code 6 chiffres (2 minutes, 5 essais puis blocage 2 minutes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> magicVerify(@Valid @RequestBody VerificationRequest request) {
        try {
            AuthResponse response = authService.magicVerify(request.email(), request.code());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", description = "Permet à un utilisateur de se connecter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identifiants invalides", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Tentative de connexion pour l'email: {}", request.email());
            String ip = getClientIp();
            String userAgent = getUserAgent();
            AuthResponse response = authService.login(request, ip, userAgent);
            log.info("Connexion réussie pour l'email: {}", request.email());
            return ResponseEntity.ok(response);
        } catch (CodeVerificationRequiredException e) {
            return ResponseEntity.status(202).body(java.util.Map.of(
                    "status", "CODE_REQUIRED",
                    "message", "Vérification par code requise",
                    "email", request.email()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage(), "message", "Identifiants invalides"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private HttpServletRequest httpServletRequest;

    private String getClientIp() {
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        String realIp = httpServletRequest.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank())
            return realIp.trim();
        return httpServletRequest.getRemoteAddr();
    }

    private String getUserAgent() {
        String ua = httpServletRequest.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token", description = "Permet de renouveler le token d'accès")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token renouvelé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erreur interne", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.debug("Renouvellement de token demandé");
            AuthResponse response = authService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Permet à un utilisateur de se déconnecter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String refreshToken = authHeader.substring(7);
                authService.logout(refreshToken);
                log.info("Déconnexion réussie");
            }
            return ResponseEntity.ok(java.util.Map.of("message", "Déconnexion réussie"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/verify-email-change")
    @Operation(summary = "Vérifier le changement d'email", description = "Vérifie le code envoyé après un changement d'email et génère un nouveau token")
    public ResponseEntity<?> verifyEmailChange(@Valid @RequestBody VerifyCodeRequest request) {
        try {
            AuthResponse response = authService.verifyEmailChange(request.code());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Renvoyer le code de vérification", description = "Renvoie un nouveau code de vérification")
    public ResponseEntity<?> resendVerification(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            authService.resendVerificationCode(userDetails.getUsername());
            return ResponseEntity.ok(java.util.Map.of("message", "Code renvoyé avec succès"));
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
