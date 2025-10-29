package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Role;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Début de l'initialisation des données par défaut...");
            // Attendre un peu pour éviter les conflits au démarrage
            Thread.sleep(500);
            initializeDefaultUsers();
            log.info("Initialisation des données par défaut terminée.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Initialisation interrompue", e);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des données par défaut", e);
            // Ne pas lancer d'exception pour ne pas empêcher le démarrage de l'application
        }
    }

    private void initializeDefaultUsers() {
        List<DefaultUser> defaults = Arrays.asList(
                new DefaultUser(
                        "admin@pneumali.ml",
                        "Admin@2024!",
                        "Admin",
                        "PneuMali",
                        "+22312345678",
                        Role.ADMIN),
                new DefaultUser(
                        "amadoulandoure004@gmail.com",
                        "F4@655#&3%@&27^!3*3o",
                        "Admin",
                        "PneuMali",
                        "+22370911112",
                        Role.ADMIN),
                new DefaultUser(
                        "dev@pneumali.ml",
                        "Dev@2024!",
                        "Developpeur",
                        "PneuMali",
                        "+22312345677",
                        Role.DEVELOPER),
                new DefaultUser(
                        "contactlandoure@gmail.com",
                        "Client#2024!",
                        "Client",
                        "Demo",
                        "+22312345679",
                        Role.CLIENT),
                new DefaultUser(
                        "phillippeterss486@gmail.com",
                        "Influenc3ur!2024",
                        "Influenceur",
                        "Demo",
                        "+22312345680",
                        Role.INFLUENCEUR));

        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (DefaultUser du : defaults) {
            try {
                String email = du.email().trim();
                var existingOpt = userRepository.findByEmailIgnoreCase(email);

                if (existingOpt.isEmpty()) {
                    User user = buildUser(du);
                    userRepository.save(user);
                    userRepository.flush();
                    created++;
                    log.info("Utilisateur par défaut créé: email={}, role={}", email, du.role());
                    continue;
                }

                User existing = existingOpt.get();

                // Ne pas toucher aux utilisateurs en cours d'authentification
                if (existing.getVerificationSentAt() != null &&
                        java.time.Instant.now().isBefore(existing.getVerificationSentAt().plusSeconds(60))) {
                    log.debug("Utilisateur {} en cours d'authentification, skip", email);
                    skipped++;
                    continue;
                }

                boolean needUpdate = false;
                if (!passwordEncoder.matches(du.rawPassword(), existing.getPassword())) {
                    existing.setPassword(passwordEncoder.encode(du.rawPassword()));
                    needUpdate = true;
                }
                if (!existing.isEnabled()) {
                    existing.setEnabled(true);
                    needUpdate = true;
                }
                if (!existing.isAccountNonLocked()) {
                    existing.setAccountNonLocked(true);
                    existing.setFailedAttempts(0);
                    existing.setLockTime(null);
                    needUpdate = true;
                }
                if (existing.getRole() != du.role()) {
                    existing.setRole(du.role());
                    needUpdate = true;
                }

                // Nettoyage de l'état OTP / vérification UNIQUEMENT pour les comptes par défaut
                // ADMIN/DEV
                if ((du.role() == Role.ADMIN || du.role() == Role.DEVELOPER) &&
                        (existing.getVerificationCode() != null || existing.getVerificationExpiry() != null
                                || existing.getVerificationSentAt() != null
                                || existing.getOtpAttempts() != null || existing.getOtpLockedUntil() != null
                                || existing.getOtpResendCount() != null)) {
                    existing.setVerificationCode(null);
                    existing.setVerificationExpiry(null);
                    existing.setVerificationSentAt(null);
                    existing.setOtpAttempts(0);
                    existing.setOtpLockedUntil(null);
                    existing.setOtpResendCount(0);
                    needUpdate = true;
                }

                if (needUpdate) {
                    userRepository.save(existing);
                    userRepository.flush();
                    updated++;
                    log.info("Utilisateur par défaut mis à jour: email={}, role={}, unlocked={}, enabled={}", email,
                            existing.getRole(), existing.isAccountNonLocked(), existing.isEnabled());
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'initialisation de l'utilisateur {}: {}", du.email(), e.getMessage());
                // Continuer avec les autres utilisateurs
            }
        }

        if (created > 0 || updated > 0 || skipped > 0) {
            log.info("Synthèse init users -> créés: {}, mis à jour: {}, ignorés: {}", created, updated, skipped);
        } else {
            log.info("Utilisateurs par défaut déjà conformes.");
        }
    }

    private User buildUser(DefaultUser du) {
        return User.builder()
                .email(du.email())
                .password(passwordEncoder.encode(du.rawPassword()))
                .firstName(du.firstName())
                .lastName(du.lastName())
                .phoneNumber(du.phone())
                .role(du.role())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .failedAttempts(0)
                .lockTime(null)
                .verificationCode(null)
                .verificationExpiry(null)
                .verificationSentAt(null)
                .otpAttempts(0)
                .otpLockedUntil(null)
                .otpResendCount(0)
                .build();
    }

    private record DefaultUser(
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            String phone,
            Role role) {
    }
}
