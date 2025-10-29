package com.pneumaliback.www.service;

import com.pneumaliback.www.service.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service unifié pour l'envoi d'emails
 * Utilise le pattern Strategy via l'interface EmailSender
 * Supporte automatiquement SMTP et SendGrid selon la configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final EmailSender emailSender;

    @Value("${app.admin.emails:}")
    private String adminEmails;

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        if (toEmail == null || toEmail.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            log.warn("Paramètres email invalides");
            return;
        }

        String subject = "Vérification de votre compte - PneuMali";
        String body = "Bonjour,\n\n"
                + "Voici votre code de vérification: " + code + "\n"
                + "Ce code expire dans 2 minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        sendEmailSafely(toEmail, subject, body, "vérification");
    }

    private void sendEmailSafely(String to, String subject, String body, String type) {
        try {
            emailSender.sendEmail(to, subject, body);
            log.info("✅ Email {} envoyé via {} à {}", type, emailSender.getProviderName(), to);
        } catch (Exception e) {
            log.error("❌ Erreur envoi email {} à {} : {}", type, to, e.getMessage());
            log.debug("Détails erreur", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String code) {
        if (toEmail == null || toEmail.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            log.warn("Paramètres email invalides");
            return;
        }

        String subject = "Réinitialisation de votre mot de passe - PneuMali";
        String body = "Bonjour,\n\n"
                + "Voici votre code de réinitialisation: " + code + "\n"
                + "Ce code expire dans 15 minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        sendEmailSafely(toEmail, subject, body, "réinitialisation");
    }

    @Async
    public void sendSuspiciousLoginAlert(String toEmail, String ip, String userAgent) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String subject = "⚠️ Alerte: connexion inhabituelle - PneuMali";
        String body = "Bonjour,\n\n"
                + "Une tentative de connexion suspecte a été détectée sur votre compte.\n\n"
                + "IP: " + (ip != null ? ip : "Inconnue") + "\n"
                + "Navigateur/Appareil: " + (userAgent != null ? userAgent : "Inconnu") + "\n\n"
                + "Si ce n'était pas vous, veuillez sécuriser votre compte immédiatement.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        sendEmailSafely(toEmail, subject, body, "alerte");
    }

    @Async
    public void sendWeeklySuspiciousReport(String subject, String body) {
        if (adminEmails == null || adminEmails.isBlank()) {
            log.debug("Aucun email admin configuré pour le rapport");
            return;
        }

        String[] recipients = adminEmails.split(",");
        for (String to : recipients) {
            String email = to.trim();
            if (!email.isEmpty()) {
                sendEmailSafely(email, subject, body, "rapport");
            }
        }
    }

    @Async
    public void sendEmailChangeNotification(String newEmail, String oldEmail) {
        if (newEmail == null || newEmail.trim().isEmpty() || oldEmail == null || oldEmail.trim().isEmpty()) {
            log.warn("Paramètres email invalides");
            return;
        }

        String subject = "PneuMali - Changement d'adresse email";
        String body = "PneuMali\n\n"
                + "L'adresse email associée à votre compte a été modifiée.\n\n"
                + "Ancienne adresse: " + oldEmail + "\n"
                + "Nouvelle adresse: " + newEmail + "\n\n"
                + "Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        sendEmailSafely(newEmail, subject, body, "changement email");
    }
}
