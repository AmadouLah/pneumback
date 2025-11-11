package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.CreateInfluenceurRequest;
import com.pneumaliback.www.dto.InfluenceurResponse;
import com.pneumaliback.www.dto.UpdateInfluenceurRequest;
import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.enums.AuthProvider;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.InfluenceurRepository;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.repository.PromotionRepository;
import com.pneumaliback.www.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluenceurService {

    private final InfluenceurRepository influenceurRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PromotionRepository promotionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public Optional<Influenceur> findByPromoCode(String code) {
        if (code == null || code.isBlank())
            return Optional.empty();
        return influenceurRepository.findByArchived(false).stream()
                .filter(i -> code.equalsIgnoreCase(i.getPromoCode()))
                .findFirst();
    }

    public List<InfluenceurResponse> findAll() {
        return influenceurRepository.findByArchived(false).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<InfluenceurResponse> findArchived() {
        return influenceurRepository.findByArchived(true).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private InfluenceurResponse toResponse(Influenceur influenceur) {
        User user = influenceur.getUser();
        return new InfluenceurResponse(
                influenceur.getId(),
                influenceur.getCommissionRate(),
                influenceur.isArchived(),
                new InfluenceurResponse.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.isEnabled()));
    }

    @Transactional
    public InfluenceurResponse createInfluenceur(CreateInfluenceurRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        // Vérifier si l'email existe déjà
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
                });

        // Créer l'utilisateur
        User user = User.builder()
                .email(normalizedEmail)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .password(passwordEncoder.encode(generateSecurePassword())) // Mot de passe temporaire
                .role(Role.INFLUENCEUR)
                .authProvider(AuthProvider.LOCAL)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(false) // Désactivé jusqu'à ce que le mot de passe soit défini
                .failedAttempts(0)
                .build();

        user = userRepository.saveAndFlush(user);

        // Créer l'influenceur
        Influenceur influenceur = new Influenceur();
        influenceur.setUser(user);
        influenceur.setCommissionRate(request.commissionRate());

        influenceur = influenceurRepository.save(influenceur);

        // Générer et envoyer le lien de définition de mot de passe
        String resetToken = generateResetToken();
        Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);

        user.setResetCode(passwordEncoder.encode(resetToken));
        user.setResetExpiry(expiry);
        user.setResetSentAt(Instant.now());
        userRepository.saveAndFlush(user);

        // Envoyer l'email de bienvenue
        mailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), resetToken);

        log.info("Influenceur créé avec succès: {} (ID: {})", normalizedEmail, influenceur.getId());
        return toResponse(influenceur);
    }

    @Transactional
    public InfluenceurResponse updateInfluenceur(Long influenceurId, UpdateInfluenceurRequest request) {
        Influenceur influenceur = influenceurRepository.findById(influenceurId)
                .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));

        User user = influenceur.getUser();

        String firstName = request.firstName() != null ? request.firstName().trim() : "";
        if (firstName.isEmpty()) {
            throw new IllegalArgumentException("Le prénom est requis");
        }

        String lastName = request.lastName() != null ? request.lastName().trim() : "";
        if (lastName.isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis");
        }

        validateCommissionRate(request.commissionRate());

        user.setFirstName(firstName);
        user.setLastName(lastName);
        influenceur.setCommissionRate(request.commissionRate());

        boolean canEditEmail = canCurrentUserEditEmail();
        if (request.email() != null) {
            String normalizedEmail = normalizeEmail(request.email());
            if (normalizedEmail.isEmpty()) {
                throw new IllegalArgumentException("L'email ne peut pas être vide");
            }

            if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
                if (!canEditEmail) {
                    throw new IllegalArgumentException("Seul un développeur peut modifier l'email de l'influenceur");
                }

                userRepository.findByEmailIgnoreCase(normalizedEmail)
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
                        });
                user.setEmail(normalizedEmail);
            }
        }

        userRepository.save(user);
        influenceurRepository.save(influenceur);

        log.info("Influenceur {} mis à jour par un utilisateur {}.", influenceurId,
                canEditEmail ? "développeur" : "administrateur");
        return toResponse(influenceur);
    }

    @Transactional
    public InfluenceurResponse toggleActive(Long influenceurId, boolean active) {
        Influenceur influenceur = influenceurRepository.findById(influenceurId)
                .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));

        if (influenceur.isArchived()) {
            throw new IllegalStateException("Impossible de changer le statut d'un influenceur archivé");
        }

        User user = influenceur.getUser();
        user.setEnabled(active);
        userRepository.save(user);

        updatePromotionsState(influenceurId, active);

        log.info("Influenceur {} {}. Promotions associées {}.", influenceurId,
                active ? "activé" : "désactivé",
                active ? "réactivées" : "désactivées");
        return toResponse(influenceur);
    }

    @Transactional
    public void deleteInfluenceur(Long influenceurId) {
        Influenceur influenceur = influenceurRepository.findById(influenceurId)
                .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));

        if (!influenceur.isArchived()) {
            throw new IllegalStateException("L'influenceur doit être archivé avant suppression définitive");
        }

        List<Promotion> promotions = promotionRepository.findByInfluenceurId(influenceurId);
        if (!promotions.isEmpty()) {
            promotionRepository.deleteAll(promotions);
        }

        refreshTokenRepository.deleteByUser(influenceur.getUser());
        userRepository.delete(influenceur.getUser());
        influenceurRepository.delete(influenceur);

        log.info("Influenceur {} supprimé et promotions associées supprimées.", influenceurId);
    }

    @Transactional
    public InfluenceurResponse archiveInfluenceur(Long influenceurId) {
        Influenceur influenceur = influenceurRepository.findById(influenceurId)
                .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));

        if (influenceur.isArchived()) {
            throw new IllegalStateException("L'influenceur est déjà archivé");
        }

        influenceur.setArchived(true);

        User user = influenceur.getUser();
        user.setEnabled(false);
        userRepository.save(user);

        updatePromotionsState(influenceurId, false);
        influenceurRepository.save(influenceur);

        log.info("Influenceur {} archivé. Compte utilisateur désactivé et promotions suspendues.", influenceurId);
        return toResponse(influenceur);
    }

    @Transactional
    public InfluenceurResponse restoreInfluenceur(Long influenceurId) {
        Influenceur influenceur = influenceurRepository.findById(influenceurId)
                .orElseThrow(() -> new IllegalArgumentException("Influenceur introuvable"));

        if (!influenceur.isArchived()) {
            throw new IllegalStateException("L'influenceur n'est pas archivé");
        }

        influenceur.setArchived(false);
        // L'utilisateur reste désactivé jusqu'à réactivation explicite
        influenceurRepository.save(influenceur);

        log.info("Influenceur {} restauré depuis l'archive.", influenceurId);
        return toResponse(influenceur);
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
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

    private void validateCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null) {
            throw new IllegalArgumentException("Le taux de commission est requis");
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le taux de commission doit être supérieur à 0");
        }
        if (commissionRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Le taux de commission ne peut pas dépasser 100%");
        }
    }

    private void updatePromotionsState(Long influenceurId, boolean active) {
        List<Promotion> promotions = promotionRepository.findByInfluenceurId(influenceurId);
        promotions.forEach(promo -> promo.setActive(active));
        if (!promotions.isEmpty()) {
            promotionRepository.saveAll(promotions);
        }
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
}
