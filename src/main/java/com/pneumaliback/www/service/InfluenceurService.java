package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.CreateInfluenceurRequest;
import com.pneumaliback.www.dto.InfluenceurResponse;
import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.AuthProvider;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.InfluenceurRepository;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Optional<Influenceur> findByPromoCode(String code) {
        if (code == null || code.isBlank())
            return Optional.empty();
        return influenceurRepository.findAll().stream()
                .filter(i -> code.equalsIgnoreCase(i.getPromoCode()))
                .findFirst();
    }

    public List<InfluenceurResponse> findAll() {
        return influenceurRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private InfluenceurResponse toResponse(Influenceur influenceur) {
        User user = influenceur.getUser();
        return new InfluenceurResponse(
                influenceur.getId(),
                influenceur.getCommissionRate(),
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

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
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
