package com.pneumaliback.www.service.mail;

import lombok.extern.slf4j.Slf4j;

/**
 * Implémentation pour développement sans service email configuré
 * Log simplement les codes dans la console au lieu d'envoyer des emails
 * Utile quand Brevo n'est pas encore configuré
 * 
 * IMPORTANT : Aucune copie n'est envoyée - uniquement le destinataire spécifié est loggé
 */
@Slf4j
public class LogOnlyEmailSender implements EmailSender {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.warn("📧 MODE DÉVELOPPEMENT - Email non envoyé (pas de service email configuré)");
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.warn("📬 Destinataire unique: {} (aucune copie)", to);
        log.warn("📌 Sujet: {}", subject);
        log.warn("📄 Corps:");
        log.warn("{}", body);
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Extraire le code de vérification du body pour faciliter les tests
        if (body.contains("code de connexion est:")) {
            String[] lines = body.split("\n");
            for (String line : lines) {
                if (line.contains("code de connexion est:")) {
                    String code = line.substring(line.indexOf(":") + 1).trim();
                    log.warn("🔑 CODE DE VÉRIFICATION: {}", code);
                    break;
                }
            }
        }
    }

    @Override
    public String getProviderName() {
        return "LogOnly (Dev Mode)";
    }
}
