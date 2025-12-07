package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.CreateLivreurRequest;
import com.pneumaliback.www.dto.LivreurResponse;
import com.pneumaliback.www.dto.UpdateLivreurRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.AuthProvider;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreurService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RefreshTokenRepository refreshTokenRepository;

    public List<LivreurResponse> findAll() {
        return userRepository.findByRole(Role.LIVREUR).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private LivreurResponse toResponse(User user) {
        return new LivreurResponse(
                user.getId(),
                new LivreurResponse.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.isEnabled()));
    }

    @Transactional
    public LivreurResponse createLivreur(CreateLivreurRequest request) {
        if (request.email() == null || request.email().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est requis");
        }

        String normalizedEmail = normalizeEmail(request.email());
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être vide");
        }

        if (isEmailAlreadyUsed(normalizedEmail, null)) {
            log.warn("Tentative de création d'un livreur avec un email existant: {}", normalizedEmail);
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .password(passwordEncoder.encode(generateSecurePassword()))
                .role(Role.LIVREUR)
                .authProvider(AuthProvider.LOCAL)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(request.active() != null ? request.active() : false)
                .failedAttempts(0)
                .build();

        user = userRepository.saveAndFlush(user);

        String resetToken = generateResetToken();
        Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);

        user.setResetCode(passwordEncoder.encode(resetToken));
        user.setResetExpiry(expiry);
        user.setResetSentAt(Instant.now());
        userRepository.saveAndFlush(user);

        mailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), resetToken);

        log.info("Livreur créé avec succès: {} (ID: {})", normalizedEmail, user.getId());
        return toResponse(user);
    }

    @Transactional
    public LivreurResponse updateLivreur(Long livreurId, UpdateLivreurRequest request) {
        User user = userRepository.findById(livreurId)
                .orElseThrow(() -> new IllegalArgumentException("Livreur introuvable"));

        if (user.getRole() != Role.LIVREUR) {
            throw new IllegalArgumentException("L'utilisateur n'est pas un livreur");
        }

        String firstName = request.firstName() != null ? request.firstName().trim() : "";
        if (firstName.isEmpty()) {
            throw new IllegalArgumentException("Le prénom est requis");
        }

        String lastName = request.lastName() != null ? request.lastName().trim() : "";
        if (lastName.isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);

        boolean canEditEmail = canCurrentUserEditEmail();
        if (request.email() != null && !request.email().trim().isEmpty()) {
            String normalizedEmail = normalizeEmail(request.email());
            if (normalizedEmail.isEmpty()) {
                throw new IllegalArgumentException("L'email ne peut pas être vide");
            }

            String currentEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
            if (!normalizedEmail.equals(currentEmail)) {
                if (!canEditEmail) {
                    throw new IllegalArgumentException("Seul un développeur peut modifier l'email du livreur");
                }

                if (isEmailAlreadyUsed(normalizedEmail, user.getId())) {
                    log.warn("Tentative de modification de l'email du livreur {} avec un email existant: {}",
                            livreurId, normalizedEmail);
                    throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
                }
                user.setEmail(normalizedEmail);
            }
        }

        userRepository.save(user);

        log.info("Livreur {} mis à jour par un utilisateur {}.", livreurId,
                canEditEmail ? "développeur" : "administrateur");
        return toResponse(user);
    }

    @Transactional
    public LivreurResponse toggleActive(Long livreurId, boolean active) {
        User user = userRepository.findById(livreurId)
                .orElseThrow(() -> new IllegalArgumentException("Livreur introuvable"));

        if (user.getRole() != Role.LIVREUR) {
            throw new IllegalArgumentException("L'utilisateur n'est pas un livreur");
        }

        user.setEnabled(active);
        userRepository.save(user);

        log.info("Livreur {} {}.", livreurId, active ? "activé" : "désactivé");
        return toResponse(user);
    }

    @Transactional
    public void deleteLivreur(Long livreurId) {
        User user = userRepository.findById(livreurId)
                .orElseThrow(() -> new IllegalArgumentException("Livreur introuvable"));

        if (user.getRole() != Role.LIVREUR) {
            throw new IllegalArgumentException("L'utilisateur n'est pas un livreur");
        }

        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);

        log.info("Livreur {} supprimé.", livreurId);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? "" : trimmed.toLowerCase();
    }

    private boolean canCurrentUserEditEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_DEVELOPER".equals(authority));
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateResetToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Vérifie si un email est déjà utilisé par un autre utilisateur
     * Utilise une requête SQL explicite avec LOWER() pour garantir la compatibilité PostgreSQL
     * 
     * @param email L'email à vérifier (doit être normalisé)
     * @param excludeUserId L'ID de l'utilisateur à exclure de la vérification (null si création)
     * @return true si l'email est déjà utilisé par un autre utilisateur
     */
    private boolean isEmailAlreadyUsed(String email, Long excludeUserId) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Utiliser la requête SQL explicite qui garantit la compatibilité avec PostgreSQL
        return userRepository.existsByEmailIgnoreCaseExcludingUser(email, excludeUserId);
    }
}
