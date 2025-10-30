package com.pneumaliback.www.configuration;

import com.pneumaliback.www.service.mail.BrevoEmailSender;
import com.pneumaliback.www.service.mail.EmailSender;
import com.pneumaliback.www.service.mail.LogOnlyEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du service email avec Brevo
 * 
 * Deux implémentations disponibles :
 * 1. Brevo API HTTP (production) - https://brevo.com
 * 2. LogOnly (développement - log dans la console)
 * 
 * Configuration via propriétés :
 * - app.mail.provider=brevo → Utilise Brevo (recommandé)
 * - app.mail.provider=logonly → Log uniquement (mode dev)
 */
@Configuration
@Slf4j
public class MailConfig {

    @Value("${app.mail.from:noreply@pneumali.ml}")
    private String fromAddress;

    /**
     * Bean EmailSender pour mode développement sans email
     * Log les codes dans la console au lieu d'envoyer des emails
     */
    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "logonly", matchIfMissing = true)
    public EmailSender logOnlyEmailSender() {
        log.warn("⚠️  Configuration email : LogOnly (Mode Développement)");
        log.warn("⚠️  Les emails seront affichés dans les logs au lieu d'être envoyés");
        return new LogOnlyEmailSender();
    }

    /**
     * Bean EmailSender pour Brevo (API HTTP)
     * Avec fallback vers LogOnly si la clé API est manquante
     */
    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
    public EmailSender brevoEmailSender(
            @Value("${app.mail.brevo.api-key:}") String apiKey) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("❌ BREVO_API_KEY non configuré ! Utilisation du mode LogOnly");
            return new LogOnlyEmailSender();
        }

        log.info("✅ Configuration email : Brevo (API HTTP) - From: {}", fromAddress);
        log.info("💡 Brevo: 300 emails gratuits/jour");
        return new BrevoEmailSender(apiKey, fromAddress);
    }
}
