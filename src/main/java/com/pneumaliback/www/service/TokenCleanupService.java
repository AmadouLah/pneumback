package com.pneumaliback.www.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    public void cleanupExpiredTokens() {
        log.info("Début du nettoyage automatique des tokens expirés...");
        try {
            refreshTokenService.cleanupExpiredTokens();
            log.info("Nettoyage automatique des tokens expirés terminé avec succès");
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage automatique des tokens expirés", e);
        }
    }
}
