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
        fixPromotionsDiscountPercentage();
        ensurePromotionActiveColumn();
        ensureInfluenceurArchivedColumn();
        ensureLivreurAssignmentEmailSentColumn();
        log.info("Migrations de base de données terminées.");
    }

    /**
     * Corrige la contrainte users_role_check pour inclure tous les rôles (ADMIN, CLIENT, INFLUENCEUR, DEVELOPER, LIVREUR)
     */
    private void fixRoleConstraint() {
        try {
            // Supprime l'ancienne contrainte si elle existe
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");

            // Crée la nouvelle contrainte avec tous les rôles incluant LIVREUR
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT users_role_check " +
                            "CHECK (role IN ('ADMIN', 'CLIENT', 'INFLUENCEUR', 'DEVELOPER', 'LIVREUR'))");

            log.info("Contrainte users_role_check mise à jour avec succès (inclut LIVREUR)");
        } catch (Exception e) {
            log.warn("Erreur lors de la mise à jour de la contrainte users_role_check: {}", e.getMessage());
            // Ne pas bloquer le démarrage si la contrainte est déjà correcte
        }
    }

    /**
     * Permet à discount_percentage d'être NULL pour les promotions de type
     * FIXED_AMOUNT
     */
    private void fixPromotionsDiscountPercentage() {
        try {
            // Vérifie si la colonne existe et a la contrainte NOT NULL
            Boolean isNotNull = jdbcTemplate.queryForObject(
                    "SELECT is_nullable = 'NO' FROM information_schema.columns " +
                            "WHERE table_name = 'promotions' AND column_name = 'discount_percentage'",
                    Boolean.class);

            if (Boolean.TRUE.equals(isNotNull)) {
                jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN discount_percentage DROP NOT NULL");
                log.info("Contrainte NOT NULL supprimée de discount_percentage dans promotions");
            } else if (isNotNull != null) {
                log.debug("discount_percentage est déjà nullable dans promotions");
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.debug("Table promotions ou colonne discount_percentage n'existe pas encore, sera créée par Hibernate");
        } catch (Exception e) {
            log.warn("Erreur lors de la mise à jour de discount_percentage dans promotions: {}", e.getMessage());
            // Ne pas bloquer le démarrage si la modification échoue
        }
    }

    /**
     * Ajoute la colonne active aux promotions si nécessaire
     */
    private void ensurePromotionActiveColumn() {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (" +
                            "SELECT 1 FROM information_schema.columns " +
                            "WHERE table_name = 'promotions' AND column_name = 'active'" +
                            ")",
                    Boolean.class);

            if (Boolean.FALSE.equals(exists)) {
                jdbcTemplate.execute("ALTER TABLE promotions ADD COLUMN active BOOLEAN");
                log.info("Colonne active ajoutée à promotions");
            }

            jdbcTemplate.execute("UPDATE promotions SET active = TRUE WHERE active IS NULL");
            jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN active SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE promotions ALTER COLUMN active SET DEFAULT TRUE");
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification/ajout de la colonne active dans promotions: {}", e.getMessage());
        }
    }

    /**
     * Ajoute la colonne archived aux influenceurs si nécessaire
     */
    private void ensureInfluenceurArchivedColumn() {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (" +
                            "SELECT 1 FROM information_schema.columns " +
                            "WHERE table_name = 'influenceurs' AND column_name = 'archived'" +
                            ")",
                    Boolean.class);

            if (Boolean.FALSE.equals(exists)) {
                jdbcTemplate.execute("ALTER TABLE influenceurs ADD COLUMN archived BOOLEAN");
                log.info("Colonne archived ajoutée à influenceurs");
            }

            jdbcTemplate.execute("UPDATE influenceurs SET archived = FALSE WHERE archived IS NULL");
            jdbcTemplate.execute("ALTER TABLE influenceurs ALTER COLUMN archived SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE influenceurs ALTER COLUMN archived SET DEFAULT FALSE");
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification/ajout de la colonne archived dans influenceurs: {}",
                    e.getMessage());
        }
    }

    /**
     * Ajoute la colonne livreur_assignment_email_sent à quote_requests si nécessaire.
     * Pour les devis existants qui ont déjà un livreur assigné, on suppose que l'email a été envoyé avec succès.
     */
    private void ensureLivreurAssignmentEmailSentColumn() {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (" +
                            "SELECT 1 FROM information_schema.columns " +
                            "WHERE table_name = 'quote_requests' AND column_name = 'livreur_assignment_email_sent'" +
                            ")",
                    Boolean.class);

            if (Boolean.FALSE.equals(exists)) {
                jdbcTemplate.execute("ALTER TABLE quote_requests ADD COLUMN livreur_assignment_email_sent BOOLEAN");
                log.info("Colonne livreur_assignment_email_sent ajoutée à quote_requests");
            }

            // Pour les devis existants qui ont déjà un livreur assigné mais livreur_assignment_email_sent = FALSE,
            // on suppose que l'email a été envoyé avec succès (car ils ont été assignés avant cette fonctionnalité)
            int updatedCount = jdbcTemplate.update(
                    "UPDATE quote_requests SET livreur_assignment_email_sent = TRUE " +
                            "WHERE assigned_livreur_id IS NOT NULL AND livreur_assignment_email_sent = FALSE");
            if (updatedCount > 0) {
                log.info("{} devis existants avec livreur assigné corrigés (email considéré comme envoyé)", updatedCount);
            }

            // Mettre à jour les valeurs NULL restantes (devis sans livreur assigné)
            jdbcTemplate.execute("UPDATE quote_requests SET livreur_assignment_email_sent = FALSE WHERE livreur_assignment_email_sent IS NULL");
            jdbcTemplate.execute("ALTER TABLE quote_requests ALTER COLUMN livreur_assignment_email_sent SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE quote_requests ALTER COLUMN livreur_assignment_email_sent SET DEFAULT FALSE");
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification/ajout de la colonne livreur_assignment_email_sent dans quote_requests: {}",
                    e.getMessage());
        }
    }
}
