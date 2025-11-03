package com.pneumaliback.www.configuration;

import com.pneumaliback.www.service.mail.BrevoEmailSender;
import com.pneumaliback.www.service.mail.EmailSender;
import com.pneumaliback.www.service.mail.LogOnlyEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.mail.from:amadoulandoure004@gmail.com}")
    private String fromAddress;

    /**
     * Bean EmailSender unique qui choisit automatiquement l'implémentation
     * selon la configuration :
     * - Si app.mail.provider=brevo → Utilise Brevo (si API key configurée)
     * - Sinon → Utilise LogOnly (mode dev ou fallback)
     */
    @Bean
    public EmailSender emailSender(
            @Value("${app.mail.provider:logonly}") String provider,
            @Value("${app.mail.brevo.api-key:}") String apiKey) {

        // Mode Brevo activé et API key présente
        if ("brevo".equalsIgnoreCase(provider) && apiKey != null && !apiKey.trim().isEmpty()) {
            log.info("✅ Configuration email : Brevo (API HTTP) - From: {}", fromAddress);
            log.info("💡 Brevo: 300 emails gratuits/jour");
            return new BrevoEmailSender(apiKey, fromAddress);
        }

        // Mode LogOnly (par défaut ou si Brevo non configuré)
        log.warn("⚠️  Configuration email : LogOnly (Mode Développement/Fallback)");
        log.warn("⚠️  Les emails seront affichés dans les logs au lieu d'être envoyés");
        if ("brevo".equalsIgnoreCase(provider)) {
            log.warn("⚠️  Provider=brevo mais BREVO_API_KEY manquante, fallback vers LogOnly");
        }
        return new LogOnlyEmailSender();
    }
}
