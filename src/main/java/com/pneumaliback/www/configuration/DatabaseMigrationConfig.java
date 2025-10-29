package com.pneumaliback.www.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Configuration pour les migrations de base de données
 * S'exécute avant DataInitializationService (Order = 0)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(0)
public class DatabaseMigrationConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Début des migrations de base de données...");
        fixRoleConstraint();
        log.info("Migrations de base de données terminées.");
    }

    /**
     * Corrige la contrainte users_role_check pour inclure DEVELOPER
     */
    private void fixRoleConstraint() {
        try {
            // Supprime l'ancienne contrainte si elle existe
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");

            // Crée la nouvelle contrainte avec tous les rôles
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT users_role_check " +
                            "CHECK (role IN ('ADMIN', 'CLIENT', 'INFLUENCEUR', 'DEVELOPER'))");

            log.info("Contrainte users_role_check mise à jour avec succès");
        } catch (Exception e) {
            log.warn("Erreur lors de la mise à jour de la contrainte users_role_check: {}", e.getMessage());
            // Ne pas bloquer le démarrage si la contrainte est déjà correcte
        }
    }
}
