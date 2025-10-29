package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.UpdateProfileRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Met à jour le profil de l'utilisateur et retourne l'utilisateur modifié
     * 
     * IMPORTANT (conforme au document Gestion.md) :
     * - Cette méthode NE CRÉE JAMAIS de nouveau compte
     * - Elle met à jour le compte EXISTANT uniquement
     * - Lors du changement d'email, toutes les données restent associées au MÊME
     * compte :
     * * Prénom et nom
     * * Adresses
     * * Commandes
     * * Historique
     * - Seul l'email change, c'est comme un "renommage" du compte
     * - Aucune duplication n'est possible
     * 
     * @return l'utilisateur mis à jour
     */
    @Transactional
    public User updateProfile(String email, UpdateProfileRequest request) {
        // Récupérer le compte existant (jamais de création)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Mise à jour du prénom (accepte les valeurs vides pour permettre de vider le
        // champ)
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }

        // Mise à jour du nom (accepte les valeurs vides pour permettre de vider le
        // champ)
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }

        // Mise à jour de l'email si fourni et différent
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()
                && !request.getEmail().trim().equals(user.getEmail())) {

            String newEmail = request.getEmail().trim();

            // Vérifier que le nouvel email n'est pas déjà utilisé par un autre compte
            if (userRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email already in use");
            }

            // Sauvegarder l'ancien email avant de le changer
            user.setPreviousEmail(user.getEmail());

            // Changer l'email du compte existant (pas de nouveau compte créé)
            // Toutes les données restent : prénom, nom, adresses, commandes, etc.
            user.setEmail(newEmail);
            user.setEnabled(false); // Désactiver temporairement jusqu'à vérification

            // Générer un code de vérification à 6 chiffres
            String plainCode = generateVerificationCode();

            // Hash le code avant de le stocker (sécurité)
            String hashedCode = passwordEncoder.encode(plainCode);
            user.setVerificationCode(hashedCode);
            user.setVerificationExpiry(Instant.now().plusSeconds(15 * 60)); // 15 minutes
            user.setVerificationSentAt(Instant.now());
            user.setOtpAttempts(0);
            user.setOtpResendCount(0);

            // Envoyer l'email de vérification avec le code en clair au NOUVEL email
            mailService.sendVerificationEmail(user.getEmail(), plainCode);
        }

        // Sauvegarder les modifications du compte existant
        return userRepository.save(user);
    }

    /**
     * Génère un code de vérification sécurisé à 6 chiffres
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
