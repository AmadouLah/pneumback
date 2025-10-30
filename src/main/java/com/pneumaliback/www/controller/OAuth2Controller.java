package com.pneumaliback.www.controller;

import com.pneumaliback.www.service.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2", description = "API d'authentification OAuth2 (Google)")
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Initie le flux OAuth2 avec Google
     * Redirige l'utilisateur vers Google pour l'authentification
     */
    @GetMapping("/google/login")
    @Operation(summary = "Initier connexion Google", description = "Redirige vers Google OAuth2")
    public void initiateGoogleLogin(HttpServletResponse response) throws IOException {
        log.info("Initiation de la connexion Google OAuth2");

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();

        response.sendRedirect(authorizationUrl);
    }

    /**
     * Callback OAuth2 de Google
     * Traite le code d'autorisation et envoie le code de vérification
     */
    @GetMapping("/callback/google")
    @Operation(summary = "Callback Google", description = "Traite le callback OAuth2 de Google")
    public void handleGoogleCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            log.error("Erreur OAuth2 de Google: {}", error);
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/login")
                    .queryParam("error", "oauth_failed")
                    .build()
                    .toUriString();
            response.sendRedirect(errorUrl);
            return;
        }

        if (code == null || code.isEmpty()) {
            log.error("Code d'autorisation manquant");
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/login")
                    .queryParam("error", "oauth_invalid")
                    .build()
                    .toUriString();
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            // Traiter le callback OAuth2
            var result = oauth2Service.processGoogleCallback(code, redirectUri);

            // Rediriger vers la page de vérification (flux Google/clients uniquement)
            String verifyUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/verify")
                    .queryParam("email", result.getEmail())
                    .queryParam("oauth", "google")
                    .build()
                    .toUriString();

            log.info("Redirection vers la page de vérification pour: {}", result.getEmail());
            response.sendRedirect(verifyUrl);

        } catch (IllegalArgumentException e) {
            // Compte LOCAL bloqué → redirection vers login avec message d'erreur
            log.warn("Compte LOCAL bloqué pour connexion Google: {}", e.getMessage());
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/login")
                    .queryParam("error", "oauth_blocked")
                    .queryParam("message", "Cette adresse email nécessite une authentification par mot de passe")
                    .build()
                    .toUriString();
            response.sendRedirect(errorUrl);
        } catch (Exception e) {
            log.error("Erreur lors du traitement du callback Google", e);
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/login")
                    .queryParam("error", "oauth_processing_error")
                    .build()
                    .toUriString();
            response.sendRedirect(errorUrl);
        }
    }
}
