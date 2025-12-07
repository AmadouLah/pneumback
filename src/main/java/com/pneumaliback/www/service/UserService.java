package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.UpdateGenderRequest;
import com.pneumaliback.www.dto.UpdateProfileRequest;
import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.AddressRepository;
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
    private final AddressRepository addressRepository;

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
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email utilisateur requis");
        }

        if (request == null) {
            throw new IllegalArgumentException("Données de mise à jour requises");
        }

        // Récupérer le compte existant (jamais de création)
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

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

        if (request.getPhoneNumber() != null) {
            String phone = request.getPhoneNumber().trim();
            user.setPhoneNumber(phone.isEmpty() ? null : phone);
        }

        // Mise à jour de l'email si fourni et différent
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()
                && !request.getEmail().trim().equalsIgnoreCase(user.getEmail())) {

            String newEmail = normalizeEmail(request.getEmail());

            // Vérifier que le nouvel email n'est pas déjà utilisé par un autre compte
            if (isEmailAlreadyUsed(newEmail, user.getId())) {
                throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
            }

            // Sauvegarder l'ancien email avant de le changer
            user.setPreviousEmail(user.getEmail());

            // Changer l'email du compte existant (pas de nouveau compte créé)
            user.setEmail(newEmail);
            user.setEnabled(false);

            // Générer et hasher le code de vérification
            String plainCode = generateVerificationCode();
            String hashedCode = passwordEncoder.encode(plainCode);
            user.setVerificationCode(hashedCode);
            user.setVerificationExpiry(Instant.now().plusSeconds(15 * 60));
            user.setVerificationSentAt(Instant.now());
            user.setOtpAttempts(0);
            user.setOtpResendCount(0);

            // Sauvegarder d'abord, puis envoyer l'email (async)
            User savedUser = userRepository.save(user);
            mailService.sendVerificationEmail(savedUser.getEmail(), plainCode);
            return savedUser;
        }

        // Sauvegarder les modifications du compte existant
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getProfile(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email utilisateur requis");
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            addressRepository.findFirstByUserAndIsDefaultTrue(user)
                    .map(Address::getPhoneNumber)
                    .filter(phone -> phone != null && !phone.isBlank())
                    .ifPresent(user::setPhoneNumber);
        }

        return user;
    }

    /**
     * Met à jour le genre de l'utilisateur
     */
    @Transactional
    public User updateGender(String email, UpdateGenderRequest request) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email utilisateur requis");
        }

        if (request == null || request.getGender() == null) {
            throw new IllegalArgumentException("Genre requis");
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        user.setGender(request.getGender());
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

    /**
     * Normalise un email en supprimant les espaces et en convertissant en minuscules
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? "" : trimmed.toLowerCase();
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
