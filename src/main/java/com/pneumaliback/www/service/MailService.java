package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.enums.PromotionType;
import com.pneumaliback.www.service.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service unifié pour l'envoi d'emails
 * Utilise le pattern Strategy via l'interface EmailSender
 * Supporte Brevo (production) et LogOnly (développement)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final EmailSender emailSender;

    private static final String DEFAULT_CONTACT_EMAIL = "amadoulandoure004@gmail.com";

    @Value("${app.admin.emails:}")
    private String adminEmails;

    @Value("${app.contact.email:}")
    private String contactEmail;

    @Value("${app.frontend-url:https://pneufront.vercel.app}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        if (toEmail == null || toEmail.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            log.warn("⚠️ Paramètres email invalides - toEmail: {}, code: {}", toEmail, code != null ? "***" : "null");
            return;
        }

        String subject = "Votre code de connexion PneuMali";
        String content = "Vous avez demandé à vous connecter à votre compte PneuMali.<br><br>"
                + "<strong style=\"font-size: 24px; color: #00d9ff; letter-spacing: 2px;\">" + code
                + "</strong><br><br>"
                + "Ce code est valide pendant 2 minutes.<br><br>"
                + "Pour votre sécurité, ne partagez jamais ce code avec qui que ce soit.<br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.</span>";
        String textBody = "Bonjour,\n\n"
                + "Vous avez demandé à vous connecter à votre compte PneuMali.\n\n"
                + "Votre code de connexion est: " + code + "\n\n"
                + "Ce code est valide pendant 2 minutes.\n\n"
                + "Pour votre sécurité, ne partagez jamais ce code avec qui que ce soit.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali\n"
                + "Votre spécialiste pneus au Mali";

        String htmlBody = buildEmailHtml("Bonjour,", content, null, null);
        sendHtmlEmailSafely(toEmail, subject, htmlBody, textBody, "vérification");
    }

    private void sendEmailSafely(String to, String subject, String body, String type) {
        sendHtmlEmailSafely(to, subject, buildEmailHtml("Bonjour,", body, null, null), body, type);
    }

    private void sendHtmlEmailSafely(String to, String subject, String htmlBody, String textBody, String type) {
        try {
            log.info("📧 Préparation envoi email HTML {} via {} à {}", type, emailSender.getProviderName(), to);
            emailSender.sendHtmlEmail(to, subject, htmlBody, textBody);
            log.info("✅ Email HTML {} CONFIRMÉ envoyé via {} à {}", type, emailSender.getProviderName(), to);
        } catch (Exception e) {
            log.error("❌ ÉCHEC envoi email HTML {} à {} via {}", type, to, emailSender.getProviderName());
            log.error("❌ Raison: {}", e.getMessage());
            log.error("❌ Stack trace:", e);
        }
    }

    /**
     * Construit un email HTML professionnel avec le style PneuMali
     * 
     * @param greeting   Salutation (ex: "Bonjour," ou "Bonjour Amadou,")
     * @param content    Contenu principal de l'email (peut contenir du HTML)
     * @param buttonText Texte du bouton (optionnel, null si pas de bouton)
     * @param buttonLink URL du bouton (optionnel, requis si buttonText est fourni)
     * @return HTML complet de l'email
     */
    private String buildEmailHtml(String greeting, String content, String buttonText, String buttonLink) {
        String buttonHtml = "";
        if (buttonText != null && buttonLink != null && !buttonText.trim().isEmpty() && !buttonLink.trim().isEmpty()) {
            buttonHtml = """
                    <table width="100%" cellpadding="0" cellspacing="0">
                        <tr>
                            <td align="center" style="padding: 20px 0;">
                                <a href=\"""" + buttonLink
                    + """
                            " style="display: inline-block; padding: 14px 32px; background-color: #00d9ff; color: #000000; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; text-align: center;">
                            """
                    + buttonText + """
                                        </a>
                                    </td>
                                </tr>
                            </table>""";
        }

        String formattedContent = content.replace("\n", "<br>");

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f5f5f5;">
                    <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td style="padding: 40px 40px 20px; text-align: center; background-color: #000000; border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #00d9ff; font-size: 28px; font-weight: bold;">PneuMali</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 30px 40px;">
                                            <p style="margin: 0 0 20px; color: #333333; font-size: 16px; line-height: 1.6;">"""
                + greeting + """
                        </p>
                        <div style="color: #333333; font-size: 16px; line-height: 1.6;">
                            """ + formattedContent + """
                        </div>
                        """ + buttonHtml
                + """
                                                    <p style="margin: 30px 0 0; color: #333333; font-size: 16px; line-height: 1.6;">
                                                        Cordialement,<br>
                                                        <strong>L'équipe PneuMali</strong>
                                                    </p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 20px 40px; background-color: #f9f9f9; border-radius: 0 0 8px 8px; text-align: center;">
                                                    <p style="margin: 0; color: #999999; font-size: 12px;">
                                                        © 2025 PneuMali. Tous droits réservés.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                        """;
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String code) {
        if (toEmail == null || toEmail.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            log.warn("Paramètres email invalides");
            return;
        }

        String subject = "Réinitialisation de votre mot de passe - PneuMali";
        String content = "Vous avez demandé à réinitialiser votre mot de passe.<br><br>"
                + "<strong style=\"font-size: 24px; color: #00d9ff; letter-spacing: 2px;\">" + code
                + "</strong><br><br>"
                + "Ce code expire dans 15 minutes.<br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.</span>";
        String textBody = "Bonjour,\n\n"
                + "Voici votre code de réinitialisation: " + code + "\n"
                + "Ce code expire dans 15 minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml("Bonjour,", content, null, null);
        sendHtmlEmailSafely(toEmail, subject, htmlBody, textBody, "réinitialisation");
    }

    @Async
    public void sendSuspiciousLoginAlert(String toEmail, String ip, String userAgent) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String subject = "⚠️ Alerte: connexion inhabituelle - PneuMali";
        String content = "<span style=\"color: #dc2626; font-weight: bold;\">Une tentative de connexion suspecte a été détectée sur votre compte.</span><br><br>"
                + "<strong>IP:</strong> " + (ip != null ? ip : "Inconnue") + "<br>"
                + "<strong>Navigateur/Appareil:</strong> " + (userAgent != null ? userAgent : "Inconnu") + "<br><br>"
                + "<span style=\"color: #dc2626;\">Si ce n'était pas vous, veuillez sécuriser votre compte immédiatement.</span>";
        String textBody = "Bonjour,\n\n"
                + "Une tentative de connexion suspecte a été détectée sur votre compte.\n\n"
                + "IP: " + (ip != null ? ip : "Inconnue") + "\n"
                + "Navigateur/Appareil: " + (userAgent != null ? userAgent : "Inconnu") + "\n\n"
                + "Si ce n'était pas vous, veuillez sécuriser votre compte immédiatement.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml("Bonjour,", content, null, null);
        sendHtmlEmailSafely(toEmail, subject, htmlBody, textBody, "alerte");
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
        String content = "L'adresse email associée à votre compte a été modifiée.<br><br>"
                + "<strong>Ancienne adresse:</strong> " + oldEmail + "<br>"
                + "<strong>Nouvelle adresse:</strong> " + newEmail + "<br><br>"
                + "<span style=\"color: #dc2626;\">Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement.</span>";
        String textBody = "Bonjour,\n\n"
                + "L'adresse email associée à votre compte a été modifiée.\n\n"
                + "Ancienne adresse: " + oldEmail + "\n"
                + "Nouvelle adresse: " + newEmail + "\n\n"
                + "Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml("Bonjour,", content, null, null);
        sendHtmlEmailSafely(newEmail, subject, htmlBody, textBody, "changement email");
    }

    @Async
    public void sendContactMessage(String senderName, String senderEmail, String phoneNumber, String message) {
        String recipients = (contactEmail != null && !contactEmail.isBlank())
                ? contactEmail
                : (adminEmails != null && !adminEmails.isBlank() ? adminEmails : DEFAULT_CONTACT_EMAIL);

        if (recipients == null || recipients.isBlank()) {
            log.warn("Aucun destinataire configuré pour les messages de contact, utilisation annulée");
            return;
        }

        String subject = "Nouvelle demande de contact - PneuMali";
        StringBuilder body = new StringBuilder()
                .append("Bonjour,\n\n")
                .append("Une nouvelle demande a été envoyée depuis le site PneuMali.\n\n")
                .append("Nom: ").append(senderName != null ? senderName : "Inconnu").append("\n")
                .append("Email: ").append(senderEmail != null ? senderEmail : "Non fourni").append("\n")
                .append("Téléphone: ")
                .append(phoneNumber != null && !phoneNumber.isBlank() ? phoneNumber : "Non communiqué").append("\n\n")
                .append("Message:\n")
                .append(message != null ? message : "(aucun message)").append("\n\n")
                .append("— Message généré automatiquement —");

        for (String to : recipients.split(",")) {
            String target = to.trim();
            if (!target.isEmpty()) {
                sendEmailSafely(target, subject, body.toString(), "contact");
            }
        }
    }

    @Async
    public void sendInfluencerPromotionAssigned(Influenceur influenceur, Promotion promotion) {
        if (influenceur == null || promotion == null || influenceur.getUser() == null) {
            log.warn("Impossible d'envoyer l'email de code promo : influenceur ou promotion invalide");
            return;
        }

        String toEmail = influenceur.getUser().getEmail();
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Aucune adresse email pour l'influenceur {}", influenceur.getId());
            return;
        }

        String firstName = influenceur.getUser().getFirstName();
        String greeting = (firstName != null && !firstName.trim().isEmpty()) ? "Bonjour " + firstName + ","
                : "Bonjour,";
        String subject = "PneuMali - Nouveau code promotionnel attribué";

        String reductionLabel = promotion.getType() == PromotionType.FIXED_AMOUNT
                ? "Montant de réduction offert"
                : "Réduction offerte à votre communauté";
        String reductionValue = formatPromotionDiscount(promotion);
        String validityValue = formatPromotionValidity(promotion.getStartDate(), promotion.getEndDate());
        String supportEmail = resolveSupportEmail();

        StringBuilder content = new StringBuilder()
                .append("Félicitations 🎉 ! Un nouveau code promo vient de vous être attribué dans le cadre du programme d’affiliation PneuMali.<br><br>")
                .append("Voici les détails de votre code :<br>")
                .append("🔹 <strong>Code promo :</strong> ").append(promotion.getCode()).append("<br>")
                .append("🔹 <strong>").append(reductionLabel).append(" :</strong> ").append(reductionValue)
                .append("<br>")
                .append("🔹 <strong>Validité :</strong> ").append(validityValue).append("<br><br>")
                .append("Vous pouvez partager ce code avec votre communauté. ")
                .append("Chaque achat effectué avec votre code sera automatiquement relié à votre compte, et vos commissions seront calculées en conséquence.<br><br>")
                .append("Suivez les performances de votre code (ventes, clics, commissions, etc.) depuis votre espace influenceur.<br><br>")
                .append("Merci de faire partie de la communauté PneuMali 🙌<br><br>")
                .append("Support : ").append(supportEmail);

        String textBody = (greeting + "\n\n"
                + "Félicitations ! Un nouveau code promo vient de vous être attribué dans le cadre du programme d’affiliation PneuMali.\n\n"
                + "Code promo : " + promotion.getCode() + "\n"
                + reductionLabel + " : " + reductionValue + "\n"
                + "Validité : " + validityValue + "\n\n"
                + "Vous pouvez partager ce code avec votre communauté.\n"
                + "Chaque achat effectué avec votre code sera automatiquement relié à votre compte, et vos commissions seront calculées en conséquence.\n\n"
                + "Suivez les performances de votre code depuis votre espace influenceur.\n\n"
                + "Merci de faire partie de la communauté PneuMali 🙌\n"
                + "Support : " + supportEmail);

        String htmlBody = buildEmailHtml(greeting, content.toString(), null, null);
        sendHtmlEmailSafely(toEmail, subject, htmlBody, textBody, "nouveau code promo influenceur");
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName, String resetToken) {
        if (toEmail == null || toEmail.trim().isEmpty() || resetToken == null || resetToken.trim().isEmpty()) {
            log.warn("Paramètres email invalides pour l'email de bienvenue");
            return;
        }

        String subject = "Bienvenue sur PneuMali - Définissez votre mot de passe";
        String greeting = firstName != null && !firstName.trim().isEmpty() ? "Bonjour " + firstName + "," : "Bonjour,";
        String baseUrl = normalizeFrontendUrl();
        String resetLink = baseUrl + "/auth/set-password?token=" + resetToken + "&email=" + toEmail;

        String content = "Bienvenue dans l'équipe PneuMali en tant qu'influenceur !<br><br>"
                + "Votre compte a été créé avec succès. Pour commencer, vous devez définir votre mot de passe.<br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Ce lien est valide pendant 7 jours.</span><br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Si vous n'avez pas demandé la création de ce compte, vous pouvez ignorer cet email.</span>";
        String textBody = greeting + "\n\n"
                + "Bienvenue dans l'équipe PneuMali en tant qu'influenceur !\n\n"
                + "Votre compte a été créé avec succès. Pour commencer, vous devez définir votre mot de passe.\n\n"
                + "Définir votre mot de passe : " + resetLink + "\n\n"
                + "Ce lien est valide pendant 7 jours.\n\n"
                + "Si vous n'avez pas demandé la création de ce compte, vous pouvez ignorer cet email.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml(greeting, content, "Définir mon mot de passe", resetLink);
        sendHtmlEmailSafely(toEmail, subject, htmlBody, textBody, "bienvenue influenceur");
    }

    private String formatPromotionDiscount(Promotion promotion) {
        if (promotion.getType() == PromotionType.FIXED_AMOUNT) {
            BigDecimal amount = promotion.getDiscountAmount() != null ? promotion.getDiscountAmount() : BigDecimal.ZERO;
            return formatCurrencyXof(amount);
        }

        BigDecimal percentage = promotion.getDiscountPercentage() != null ? promotion.getDiscountPercentage()
                : BigDecimal.ZERO;
        return percentage.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatPromotionValidity(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String start = startDate != null ? startDate.format(formatter) : "immédiatement";
        if (endDate == null) {
            return "à partir du " + start + " (sans date de fin)";
        }
        return "du " + start + " au " + endDate.format(formatter);
    }

    private String formatCurrencyXof(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return safeAmount.stripTrailingZeros().toPlainString() + " XOF";
    }

    private String normalizeFrontendUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "https://pneumali.com";
        }
        return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    private String resolveSupportEmail() {
        if (contactEmail != null && !contactEmail.isBlank()) {
            return contactEmail.trim();
        }
        if (adminEmails != null && !adminEmails.isBlank()) {
            return adminEmails.split(",")[0].trim();
        }
        return DEFAULT_CONTACT_EMAIL;
    }
}
