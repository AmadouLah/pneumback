package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.AuthResponse;
import com.pneumaliback.www.dto.LoginRequest;
import com.pneumaliback.www.dto.RegisterRequest;
import com.pneumaliback.www.dto.MessageResponse;
import com.pneumaliback.www.dto.ResendVerificationRequest;
import com.pneumaliback.www.dto.ForgotPasswordRequest;
import com.pneumaliback.www.dto.ResetPasswordRequest;
import com.pneumaliback.www.dto.VerificationRequest;
import com.pneumaliback.www.dto.StartLoginResponse;
import com.pneumaliback.www.entity.RefreshToken;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.exception.CodeVerificationRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final MailService mailService;
    private final AuditService auditService;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        throw new RuntimeException("Inscription classique désactivée. Utilisez la connexion par e-mail.");
    }

    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        try {
            String email = request.email() == null ? "" : request.email().trim();
            User userForRole = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Identifiants invalides"));
            if (userForRole.getRole() != Role.ADMIN && userForRole.getRole() != Role.DEVELOPER) {
                throw new RuntimeException(
                        "La connexion par mot de passe est réservée aux administrateurs et développeurs. Utilisez la connexion par e-mail.");
            }
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (requiresCodeVerification(user, ip, userAgent)) {
                sendVerificationCode(user, true);

                // Email d'alerte UNIQUEMENT si changement suspect (changement IP/device)
                boolean isSuspicious = isSuspiciousLogin(user, ip, userAgent);
                if (isSuspicious) {
                    auditService.logAuthEvent("SUSPICIOUS_LOGIN", user.getEmail(), ip, userAgent,
                            java.util.Map.of("reason", "DEVICE_OR_IP_CHANGED"));
                    mailService.sendSuspiciousLoginAlert(user.getEmail(), ip, userAgent);
                } else {
                    auditService.logAuthEvent("2FA_REQUIRED", user.getEmail(), ip, userAgent,
                            java.util.Map.of("reason", "ADMIN_OR_DEVELOPER_STANDARD_2FA"));
                }

                throw new CodeVerificationRequiredException("CODE_REQUIRED");
            }

            // Ce code ne devrait jamais être atteint car ADMIN et DEVELOPER ont toujours
            // 2FA
            // Conservé uniquement pour cohérence du flux (en cas de modification future)
            throw new RuntimeException("Configuration de sécurité incorrecte");

        } catch (DisabledException e) {
            throw new RuntimeException("Compte non activé. Veuillez vérifier votre email.");
        } catch (BadCredentialsException e) {
            handleFailedLogin(request.email());
            auditService.logAuthEvent("LOGIN_FAILED", request.email(), ip, userAgent,
                    java.util.Map.of("cause", "BAD_CREDENTIALS"));
            throw new RuntimeException("Email ou mot de passe incorrect");
        } catch (LockedException e) {
            throw new RuntimeException("Compte verrouillé. Réessayez plus tard.");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token invalide"));

        token = refreshTokenService.verifyExpiration(token);

        UserDetails userDetails = userDetailsService.loadUserByUsername(token.getUser().getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        return buildAuthResponse(newAccessToken, refreshToken, token.getUser());
    }

    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token invalide"));

        refreshTokenService.revokeByUser(token.getUser());
    }

    /**
     * Renvoie un code de vérification à un utilisateur existant
     * Cette méthode ne crée JAMAIS d'utilisateur, elle travaille uniquement avec
     * des comptes existants
     * 
     * Cas d'usage typiques :
     * - L'utilisateur n'a pas reçu le premier code
     * - Le code a expiré
     * - L'utilisateur veut se reconnecter après expiration du token
     */
    @Transactional
    public MessageResponse resendVerificationCode(ResendVerificationRequest request) {
        // Récupérer l'utilisateur existant (jamais de création)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que le compte n'est pas déjà activé et connecté
        if (user.isEnabled()) {
            throw new RuntimeException("Compte déjà activé");
        }

        Instant now = Instant.now();

        // Réinitialiser le compteur si le code précédent a expiré
        if (user.getVerificationExpiry() != null && now.isAfter(user.getVerificationExpiry())) {
            user.setOtpResendCount(0);
            user.setOtpAttempts(0);
            user.setOtpLockedUntil(null);
            log.debug("Code expiré, réinitialisation des compteurs pour : {}", request.email());
        }

        // Validation du cooldown anti-spam
        if (user.getVerificationSentAt() != null && now.isBefore(user.getVerificationSentAt().plusSeconds(20))) {
            throw new RuntimeException("Veuillez patienter avant de renvoyer le code");
        }

        // Déterminer si c'est un premier envoi ou un renvoi
        Integer count = user.getOtpResendCount() == null ? 0 : user.getOtpResendCount();
        boolean isFirstSend = (count == 0) || (user.getVerificationCode() == null);

        // Vérifier la limite de renvois (le premier envoi ne compte pas)
        if (!isFirstSend && count >= 3) {
            throw new RuntimeException("Nombre maximum de renvois atteint. Veuillez attendre que le code expire.");
        }

        // Incrémenter uniquement si c'est un renvoi (pas le premier envoi)
        if (!isFirstSend) {
            user.setOtpResendCount(count + 1);
        }

        userRepository.saveAndFlush(user);
        sendVerificationCode(user, true);

        log.info("Code de vérification renvoyé pour l'utilisateur : {}", user.getEmail());
        return new MessageResponse("Nouveau code envoyé à votre email.");
    }

    /**
     * Renvoie le code de vérification pour l'utilisateur connecté
     * Utilisé par exemple lors du changement d'email
     * Cette méthode ne crée JAMAIS d'utilisateur, elle travaille uniquement avec
     * des comptes existants
     */
    @Transactional
    public void resendVerificationCode(String email) {
        // Récupérer l'utilisateur existant (jamais de création)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Instant now = Instant.now();

        // Réinitialiser le compteur si le code précédent a expiré
        if (user.getVerificationExpiry() != null && now.isAfter(user.getVerificationExpiry())) {
            user.setOtpResendCount(0);
            user.setOtpAttempts(0);
            user.setOtpLockedUntil(null);
            log.debug("Code expiré, réinitialisation des compteurs pour : {}", email);
        }

        // Validation du cooldown anti-spam
        if (user.getVerificationSentAt() != null && now.isBefore(user.getVerificationSentAt().plusSeconds(20))) {
            throw new RuntimeException("Veuillez patienter avant de renvoyer le code");
        }

        // Déterminer si c'est un premier envoi ou un renvoi
        Integer count = user.getOtpResendCount() == null ? 0 : user.getOtpResendCount();
        boolean isFirstSend = (count == 0) || (user.getVerificationCode() == null);

        // Vérifier la limite de renvois (le premier envoi ne compte pas)
        if (!isFirstSend && count >= 3) {
            throw new RuntimeException("Nombre maximum de renvois atteint. Veuillez attendre que le code expire.");
        }

        // Incrémenter uniquement si c'est un renvoi (pas le premier envoi)
        if (!isFirstSend) {
            user.setOtpResendCount(count + 1);
        }

        userRepository.saveAndFlush(user);
        sendVerificationCode(user, true);

        log.info("Code de vérification renvoyé pour l'utilisateur connecté : {}", user.getEmail());
    }

    /**
     * Vérifie le code OTP et connecte l'utilisateur
     * Cette méthode NE CRÉE JAMAIS d'utilisateur - elle vérifie simplement le code
     * Gère à la fois la première connexion et les reconnexions
     * 
     * Conforme au document Gestion.md :
     * - Ne recrée JAMAIS un compte existant
     * - Génère simplement un nouveau token après validation du code
     * - Réinitialise les compteurs de sécurité
     */
    @Transactional
    public AuthResponse verifyEmail(VerificationRequest request) {
        // Étape 1 : Récupérer l'utilisateur existant (jamais de création ici)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Étape 2 : Vérifier qu'un code a bien été envoyé
        if (user.getVerificationCode() == null || user.getVerificationExpiry() == null) {
            throw new RuntimeException("Aucun code de vérification actif");
        }

        // Étape 3 : Vérifier si le compte est temporairement verrouillé
        // (anti-bruteforce)
        if (user.getOtpLockedUntil() != null && Instant.now().isBefore(user.getOtpLockedUntil())) {
            throw new RuntimeException("Trop de tentatives. Réessayez plus tard");
        }

        // Étape 4 : Vérifier l'expiration du code
        if (Instant.now().isAfter(user.getVerificationExpiry())) {
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            user.setVerificationSentAt(null);
            user.setOtpAttempts(0);
            user.setOtpLockedUntil(null);
            user.setOtpResendCount(0);
            userRepository.saveAndFlush(user);
            throw new RuntimeException("Code de vérification expiré");
        }

        // Étape 5 : Vérifier la validité du code
        boolean codeOk = passwordEncoder.matches(request.code(), user.getVerificationCode());
        if (!codeOk) {
            int attempts = user.getOtpAttempts() == null ? 0 : user.getOtpAttempts();
            attempts++;
            user.setOtpAttempts(attempts);
            if (attempts >= 5) {
                user.setOtpLockedUntil(Instant.now().plus(2, ChronoUnit.MINUTES));
                user.setOtpAttempts(0);
                auditService.logAuthEvent("CODE_LOCK", user.getEmail(), null, null,
                        java.util.Map.of("reason", "TOO_MANY_ATTEMPTS"));
            }
            userRepository.saveAndFlush(user);
            auditService.logAuthEvent("CODE_INVALID", user.getEmail(), null, null, null);
            throw new RuntimeException("Code de vérification invalide");
        }

        // Étape 6 : Code valide → Activer le compte et nettoyer les données OTP
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationExpiry(null);
        user.setVerificationSentAt(null);
        user.setOtpAttempts(0);
        user.setOtpLockedUntil(null);
        user.setOtpResendCount(0);
        userRepository.saveAndFlush(user);

        // Étape 7 : Générer un nouveau token JWT (que ce soit première connexion ou
        // reconnexion)
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponse auth = buildAuthResponse(accessToken, refreshToken.getToken(), user);
        auditService.logAuthEvent("CODE_VERIFIED", user.getEmail(), null, null, null);
        log.info("Connexion réussie pour l'utilisateur : {}", user.getEmail());
        return auth;
    }

    private void handleFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedAttempts(user.getFailedAttempts() + 1);

            if (user.getFailedAttempts() >= 5) {
                user.setAccountNonLocked(false);
                user.setLockTime(Instant.now());
            }

            userRepository.saveAndFlush(user);
        });
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                86400000L, // 24 heures
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole().name()));
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    private void sendVerificationCode(User user, boolean isResend) {
        Instant now = Instant.now();
        if (!isResend) {
            user.setOtpAttempts(0);
            user.setOtpLockedUntil(null);
            user.setOtpResendCount(0);
        }
        String code = generateVerificationCode();
        Instant expiry = now.plus(2, ChronoUnit.MINUTES);
        String hash = passwordEncoder.encode(code);
        user.setVerificationCode(hash);
        user.setVerificationExpiry(expiry);
        user.setVerificationSentAt(now);
        userRepository.saveAndFlush(user);
        mailService.sendVerificationEmail(user.getEmail(), code);
        auditService.logAuthEvent("MAGIC_CODE_SENT", user.getEmail(), null, null, null);
    }

    /**
     * Point d'entrée unique pour l'authentification par email (magic link)
     * Gère à la fois l'inscription ET la connexion selon le document Gestion.md
     * 
     * Logique :
     * - Si l'email n'existe pas → INSCRIPTION (création du compte)
     * - Si l'email existe déjà → CONNEXION (réutilisation du compte existant)
     * - Dans les DEUX cas, on envoie un code OTP
     * - Le compte n'est JAMAIS recréé, même après expiration du token
     * 
     * Limites de sécurité :
     * - Cooldown de 20s entre chaque envoi
     * - Maximum 3 renvois (après le premier envoi initial)
     * - Réinitialisation automatique après expiration du code
     */
    @Transactional
    public MessageResponse magicStart(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isEmpty())
            throw new RuntimeException("Email requis");

        // Étape 1 : Vérifier si l'utilisateur existe déjà
        var existingUser = userRepository.findByEmail(normalized);
        boolean isNewUser = existingUser.isEmpty();

        User user;
        if (isNewUser) {
            // CAS 1 : INSCRIPTION - Créer un nouveau compte (une seule fois)
            log.info("Nouvelle inscription pour l'email : {}", normalized);
            user = User.builder()
                    .email(normalized)
                    .password(passwordEncoder.encode(generateVerificationCode()))
                    .firstName("")
                    .lastName("")
                    .role(Role.CLIENT)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .enabled(false)
                    .failedAttempts(0)
                    .build();
            user = userRepository.saveAndFlush(user);
            auditService.logAuthEvent("NEW_USER_REGISTRATION", user.getEmail(), null, null, null);
        } else {
            // CAS 2 : CONNEXION - Réutiliser le compte existant (pas de duplication)
            user = existingUser.get();
            log.info("Reconnexion pour l'utilisateur existant : {}", normalized);
            auditService.logAuthEvent("EXISTING_USER_RECONNECTION", user.getEmail(), null, null, null);
        }

        Instant now = Instant.now();

        // Étape 2 : Réinitialiser le compteur si le code précédent a expiré
        if (user.getVerificationExpiry() != null && now.isAfter(user.getVerificationExpiry())) {
            user.setOtpResendCount(0);
            user.setOtpAttempts(0);
            user.setOtpLockedUntil(null);
            log.debug("Code expiré, réinitialisation des compteurs pour : {}", normalized);
        }

        // Étape 3 : Validation du cooldown (anti-spam)
        if (user.getVerificationSentAt() != null && now.isBefore(user.getVerificationSentAt().plusSeconds(20))) {
            throw new RuntimeException("Veuillez patienter avant de renvoyer le code");
        }

        // Étape 4 : Déterminer si c'est un premier envoi ou un renvoi
        Integer count = user.getOtpResendCount() == null ? 0 : user.getOtpResendCount();
        boolean isFirstSend = (count == 0) || (user.getVerificationCode() == null);

        // Étape 5 : Vérifier la limite de renvois (le premier envoi ne compte pas)
        if (!isFirstSend && count >= 3) {
            throw new RuntimeException("Nombre maximum de renvois atteint. Veuillez attendre que le code expire.");
        }

        // Étape 6 : Générer et envoyer le code
        String code = generateVerificationCode();
        String hash = passwordEncoder.encode(code);
        Instant expiry = now.plus(2, ChronoUnit.MINUTES);

        // Mise à jour de tous les champs en une seule fois
        if (isFirstSend) {
            user.setOtpAttempts(0);
            user.setOtpLockedUntil(null);
            user.setOtpResendCount(0); // Le premier envoi ne compte pas comme renvoi
        } else {
            user.setOtpResendCount(count + 1); // Incrémenter uniquement pour les renvois
        }

        user.setVerificationCode(hash);
        user.setVerificationExpiry(expiry);
        user.setVerificationSentAt(now);

        // UNE SEULE sauvegarde
        userRepository.saveAndFlush(user);

        // Envoi du mail (après la sauvegarde)
        mailService.sendVerificationEmail(user.getEmail(), code);

        auditService.logAuthEvent("MAGIC_CODE_SENT", user.getEmail(), null, null,
                java.util.Map.of("isNewUser", isNewUser, "isFirstSend", isFirstSend, "resendCount",
                        user.getOtpResendCount()));

        return new MessageResponse("Code envoyé");
    }

    public AuthResponse magicVerify(String email, String code) {
        VerificationRequest req = new VerificationRequest(email, code);
        return verifyEmail(req);
    }

    @Transactional
    public StartLoginResponse startLogin(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isEmpty())
            throw new RuntimeException("Email requis");
        var userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isPresent()) {
            Role role = userOpt.get().getRole();
            if (role == Role.ADMIN || role == Role.DEVELOPER) {
                return new StartLoginResponse("ADMIN_PASSWORD", "Connexion par mot de passe");
            }
        }
        magicStart(normalized);
        return new StartLoginResponse("EMAIL_CODE", "Code envoyé par e-mail");
    }

    @Transactional
    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Compte non activé");
        }

        Instant now = Instant.now();
        if (user.getResetSentAt() != null && now.isBefore(user.getResetSentAt().plusSeconds(20))) {
            throw new RuntimeException("Veuillez patienter avant de renvoyer le code");
        }

        sendNewResetCode(user);
        return new MessageResponse("Un code de réinitialisation a été envoyé à votre email.");
    }

    @Transactional
    public MessageResponse confirmPasswordReset(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Compte non activé");
        }

        if (user.getResetCode() == null || user.getResetExpiry() == null) {
            throw new RuntimeException("Aucun code de réinitialisation actif");
        }

        if (!passwordEncoder.matches(request.code(), user.getResetCode())) {
            throw new RuntimeException("Code de réinitialisation invalide");
        }

        if (Instant.now().isAfter(user.getResetExpiry())) {
            throw new RuntimeException("Code de réinitialisation expiré");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetCode(null);
        user.setResetExpiry(null);
        user.setResetSentAt(null);
        userRepository.saveAndFlush(user);

        return new MessageResponse("Mot de passe réinitialisé avec succès.");
    }

    private void sendNewResetCode(User user) {
        String code = generateVerificationCode();
        Instant expiry = Instant.now().plus(15, ChronoUnit.MINUTES);
        String hash = passwordEncoder.encode(code);
        user.setResetCode(hash);
        user.setResetExpiry(expiry);
        user.setResetSentAt(Instant.now());
        userRepository.saveAndFlush(user);
        mailService.sendPasswordResetEmail(user.getEmail(), code);
        auditService.logAuthEvent("PASSWORD_RESET_REQUEST", user.getEmail(), null, null, null);
    }

    /**
     * Détermine si la vérification par code 2FA est requise
     * 
     * ADMIN et DEVELOPER : 2FA obligatoire à CHAQUE connexion (sécurité maximale)
     * CLIENT/INFLUENCEUR : Pas de 2FA (connexion par magic link uniquement)
     */
    private boolean requiresCodeVerification(User user, String ip, String userAgent) {
        // ADMIN et DEVELOPER : toujours 2FA à chaque connexion
        return user.getRole() == Role.ADMIN || user.getRole() == Role.DEVELOPER;
    }

    /**
     * Détermine si une connexion est suspecte (changement device/IP)
     * 
     * DIFFÉRENCE avec requiresCodeVerification :
     * - requiresCodeVerification : TRUE pour première connexion OU changement
     * device
     * - isSuspiciousLogin : TRUE UNIQUEMENT pour changement device (pas première
     * connexion)
     * 
     * L'email d'alerte ne doit être envoyé QUE si c'est vraiment suspect !
     */
    private boolean isSuspiciousLogin(User user, String ip, String userAgent) {
        // Première connexion : ce n'est PAS suspect, c'est normal !
        if (user.getLastLoginIp() == null || user.getLastLoginUserAgent() == null) {
            return false;
        }

        // Changement d'IP ou de User-Agent : c'est suspect !
        boolean ipChanged = !user.getLastLoginIp().equals(ip);
        boolean uaChanged = !user.getLastLoginUserAgent().equals(userAgent);
        return ipChanged || uaChanged;
    }

    /**
     * Vérifie le code de vérification (utilisé pour le changement d'email)
     * Retourne l'utilisateur pour générer un nouveau token
     * 
     * Important : Cette méthode NE CRÉE JAMAIS d'utilisateur
     * Elle cherche parmi les utilisateurs existants celui qui correspond au code
     * fourni
     * Utilisée principalement lors du changement d'email où on ne connaît pas
     * encore le nouvel email
     */
    @Transactional
    public User verifyCode(String code) {
        // Récupérer tous les utilisateurs existants avec un code de vérification actif
        java.util.List<User> usersWithCode = userRepository.findAll().stream()
                .filter(u -> u.getVerificationCode() != null && u.getVerificationExpiry() != null)
                .filter(u -> Instant.now().isBefore(u.getVerificationExpiry()))
                .toList();

        // Chercher l'utilisateur dont le code hash correspond
        User user = null;
        for (User u : usersWithCode) {
            if (passwordEncoder.matches(code, u.getVerificationCode())) {
                user = u;
                break;
            }
        }

        if (user == null) {
            throw new RuntimeException("Code invalide ou expiré");
        }

        // Réactiver le compte et nettoyer les données OTP
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationExpiry(null);
        user.setVerificationSentAt(null);
        user.setOtpAttempts(0);
        user.setOtpResendCount(0);
        user.setOtpLockedUntil(null);

        log.info("Code vérifié avec succès pour l'utilisateur : {}", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * Vérifie le changement d'email et génère un nouveau token
     * 
     * Cette méthode NE CRÉE JAMAIS d'utilisateur :
     * - L'utilisateur existe déjà avec son ancien email
     * - On vérifie simplement le code
     * - On met à jour l'email (pas de nouveau compte)
     * - On régénère un token pour le compte existant
     * 
     * Conforme au document Gestion.md : toutes les données (adresses, commandes,
     * etc.)
     * restent associées au MÊME compte utilisateur, seul l'email change
     */
    @Transactional
    public AuthResponse verifyEmailChange(String code) {
        // Vérifier le code (utilisateur existant uniquement)
        User user = verifyCode(code);

        // Envoyer l'email de notification au nouveau compte
        if (user.getPreviousEmail() != null && !user.getPreviousEmail().isEmpty()) {
            mailService.sendEmailChangeNotification(user.getEmail(), user.getPreviousEmail());
            // Nettoyer le champ previousEmail après l'envoi
            user.setPreviousEmail(null);
            userRepository.save(user);
        }

        // Charger les UserDetails avec le nouvel email
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Générer un nouveau token pour le MÊME utilisateur (pas de nouveau compte)
        // Note: createRefreshToken supprime automatiquement les anciens tokens
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Email changé avec succès pour l'utilisateur ID: {} - Ancien: {} - Nouveau: {}",
                user.getId(), user.getPreviousEmail(), user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                86400L,
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole().name()));
    }
}
