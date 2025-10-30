package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Traite le callback OAuth2 de Google
     * Échange le code d'autorisation contre les informations utilisateur
     * Crée ou récupère l'utilisateur et envoie un code de vérification
     * 
     * @param code        Code d'autorisation de Google
     * @param redirectUri URI de redirection
     * @return Email de l'utilisateur
     */
    @Transactional
    public String processGoogleCallback(String code, String redirectUri) {
        log.info("Traitement du callback Google OAuth2");

        // Étape 1 : Échanger le code contre un access token
        String accessToken = exchangeCodeForToken(code, redirectUri);

        // Étape 2 : Récupérer les informations utilisateur de Google
        Map<String, Object> userInfo = getUserInfoFromGoogle(accessToken);

        // Étape 3 : Extraire les informations
        String email = (String) userInfo.get("email");
        String firstName = (String) userInfo.get("given_name");
        String lastName = (String) userInfo.get("family_name");
        String googleId = (String) userInfo.get("sub");

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email non fourni par Google");
        }

        log.info("Authentification Google réussie pour: {}", email);

        // Étape 4 : Créer ou récupérer l'utilisateur
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Création d'un nouvel utilisateur via Google OAuth2: {}", email);
            User newUser = User.builder()
                    .email(email)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .googleId(googleId)
                    .password(passwordEncoder.encode(generateSecurePassword()))
                    .role(Role.CLIENT)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .enabled(false) // Nécessite vérification du code
                    .failedAttempts(0)
                    .build();
            return userRepository.save(newUser);
        });

        // Si l'utilisateur existe mais n'a pas de googleId, l'ajouter
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }

        // Mettre à jour les informations si nécessaire
        if ((firstName != null && !firstName.equals(user.getFirstName())) ||
                (lastName != null && !lastName.equals(user.getLastName()))) {
            if (firstName != null)
                user.setFirstName(firstName);
            if (lastName != null)
                user.setLastName(lastName);
        }

        // Étape 5 : Générer et envoyer le code de vérification
        sendVerificationCode(user);

        auditService.logAuthEvent("GOOGLE_OAUTH_INITIATED", email, null, null,
                Map.of("googleId", googleId));

        return email;
    }

    /**
     * Échange le code d'autorisation contre un access token
     */
    @SuppressWarnings("unchecked")
    private String exchangeCodeForToken(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("access_token")) {
                throw new RuntimeException("Token d'accès non reçu de Google");
            }

            return (String) body.get("access_token");
        } catch (Exception e) {
            log.error("Erreur lors de l'échange du code contre un token", e);
            throw new RuntimeException("Échec de l'authentification Google", e);
        }
    }

    /**
     * Récupère les informations utilisateur depuis Google
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserInfoFromGoogle(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null) {
                throw new RuntimeException("Informations utilisateur non reçues de Google");
            }

            return userInfo;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des informations utilisateur", e);
            throw new RuntimeException("Échec de la récupération des informations Google", e);
        }
    }

    /**
     * Génère et envoie le code de vérification
     */
    private void sendVerificationCode(User user) {
        String code = generateVerificationCode();
        Instant expiry = Instant.now().plus(2, ChronoUnit.MINUTES);
        String hash = passwordEncoder.encode(code);

        user.setVerificationCode(hash);
        user.setVerificationExpiry(expiry);
        user.setVerificationSentAt(Instant.now());
        user.setOtpAttempts(0);
        user.setOtpResendCount(0);
        user.setOtpLockedUntil(null);

        userRepository.saveAndFlush(user);
        mailService.sendVerificationEmail(user.getEmail(), code);

        log.info("Code de vérification envoyé à: {}", user.getEmail());
    }

    /**
     * Génère un code de vérification à 6 chiffres
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    /**
     * Génère un mot de passe aléatoire sécurisé
     */
    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
