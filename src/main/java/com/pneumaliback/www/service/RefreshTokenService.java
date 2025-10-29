package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.RefreshToken;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}") // 7 jours par défaut
    private long refreshTokenDurationMs;

    /**
     * Crée un nouveau refresh token pour l'utilisateur
     * 
     * IMPORTANT (conforme au document Gestion.md) :
     * - Supprime TOUJOURS les tokens existants AVANT de créer un nouveau
     * - Un utilisateur ne peut avoir qu'UN SEUL refresh token actif à la fois
     * - Évite les erreurs de contrainte unique sur user_id
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Supprimer d'abord tous les tokens existants pour cet utilisateur
        // (conformément au principe : pas de doublons)
        deleteByUser(user);

        // Créer le nouveau token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        log.info("Nouveau refresh token créé pour l'utilisateur ID: {}", user.getId());
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expiré. Veuillez vous reconnecter.");
        }
        return token;
    }

    /**
     * Supprime tous les refresh tokens d'un utilisateur
     * Utilisé pour nettoyer avant de créer un nouveau token
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.debug("Tokens supprimés pour l'utilisateur ID: {}", user.getId());
    }

    /**
     * Révoque tous les refresh tokens d'un utilisateur
     * Utilisé lors de la déconnexion
     */
    @Transactional
    public void revokeByUser(User user) {
        refreshTokenRepository.revokeByUser(user);
        log.debug("Tokens révoqués pour l'utilisateur ID: {}", user.getId());
    }

    @Scheduled(fixedRate = 86400000) // Exécution quotidienne
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Nettoyage des refresh tokens expirés...");
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Nettoyage terminé");
    }
}
