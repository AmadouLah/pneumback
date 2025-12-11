package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.PromotionType;
import com.pneumaliback.www.repository.UserRepository;
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
    private final UserRepository userRepository;

    private static final String DEFAULT_CONTACT_EMAIL = "amadoulandoure004@gmail.com";

    @Value("${app.admin.emails:}")
    private String adminEmails;

    @Value("${app.contact.email:}")
    private String contactEmail;

    @Value("${app.frontend-url:https://pneufront.vercel.app}")
    private String frontendUrl;

    /**
     * Envoie un code de vérification par email.
     * IMPORTANT : Le code est envoyé UNIQUEMENT au destinataire spécifié, sans
     * aucune copie (CC/BCC).
     * 
     * @param toEmail Adresse email du destinataire (unique)
     * @param code    Code de vérification à envoyer
     */
    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        if (toEmail == null || toEmail.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            log.warn("⚠️ Paramètres email invalides - toEmail: {}, code: {}", toEmail, code != null ? "***" : "null");
            return;
        }

        String subject = "Votre code de connexion PneuMali";
        User user = userRepository.findByEmailIgnoreCase(toEmail).orElse(null);
        String greeting = buildGreeting(user);

        String content = "Vous avez demandé à vous connecter à votre compte PneuMali.<br><br>"
                + "<strong style=\"font-size: 24px; color: #00d9ff; letter-spacing: 2px;\">" + code
                + "</strong><br><br>"
                + "Ce code est valide pendant 2 minutes.<br><br>"
                + "Pour votre sécurité, ne partagez jamais ce code avec qui que ce soit.<br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.</span>";

        String textBody = "Vous avez demandé à vous connecter à votre compte PneuMali.\n\n"
                + "Votre code de connexion est: " + code + "\n\n"
                + "Ce code est valide pendant 2 minutes.\n\n"
                + "Pour votre sécurité, ne partagez jamais ce code avec qui que ce soit.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali\n"
                + "Votre spécialiste pneus au Mali";

        String htmlBody = buildEmailHtml(greeting, content, null, null);
        // Envoi sécurisé : uniquement au destinataire, aucune copie
        sendHtmlEmailSafely(toEmail, subject, htmlBody, textBody, "vérification");
    }

    private void sendEmailSafely(String to, String subject, String body, String type) {
        sendHtmlEmailSafely(to, subject, buildEmailHtml("", body, null, null), body, type);
    }

    /**
     * Envoie un email HTML de manière sécurisée.
     * Garantit qu'aucune copie (CC/BCC) n'est envoyée - uniquement le destinataire
     * spécifié.
     * 
     * @param to       Destinataire unique
     * @param subject  Sujet de l'email
     * @param htmlBody Corps HTML
     * @param textBody Corps texte (fallback)
     * @param type     Type d'email pour les logs
     */
    private void sendHtmlEmailSafely(String to, String subject, String htmlBody, String textBody, String type) {
        sendHtmlEmailSafelySync(to, subject, htmlBody, textBody, type);
    }

    /**
     * Version synchrone de sendHtmlEmailSafely qui retourne le résultat de l'envoi.
     * 
     * @param to       Destinataire unique
     * @param subject  Sujet de l'email
     * @param htmlBody Corps HTML
     * @param textBody Corps texte (fallback)
     * @param type     Type d'email pour les logs
     * @return true si l'email a été envoyé avec succès, false sinon
     */
    private boolean sendHtmlEmailSafelySync(String to, String subject, String htmlBody, String textBody, String type) {
        try {
            log.info("📧 Préparation envoi email HTML {} via {} à {} (destinataire unique uniquement)",
                    type, emailSender.getProviderName(), to);
            emailSender.sendHtmlEmail(to, subject, htmlBody, textBody);
            log.info("✅ Email HTML {} CONFIRMÉ envoyé via {} à {} (aucune copie)",
                    type, emailSender.getProviderName(), to);
            return true;
        } catch (Exception e) {
            log.error("❌ ÉCHEC envoi email HTML {} à {} via {}", type, to, emailSender.getProviderName());
            log.error("❌ Raison: {}", e.getMessage());
            log.error("❌ Stack trace:", e);
            return false;
        }
    }

    /**
     * Construit un email HTML professionnel avec le style PneuMali
     * 
     * @param greeting   Salutation (non utilisée, conservée pour compatibilité)
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
        String greetingHtml = (greeting != null && !greeting.trim().isEmpty())
                ? "<p style=\"margin: 0 0 20px; color: #333333; font-size: 16px; line-height: 1.6;\">" + greeting
                        + "</p>"
                : "";

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
                                            """
                + greetingHtml + """
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

    /**
     * Envoie un code de réinitialisation de mot de passe par email.
     * IMPORTANT : Le code est envoyé UNIQUEMENT au destinataire spécifié, sans
     * aucune copie (CC/BCC).
     * 
     * @param toEmail Adresse email du destinataire (unique)
     * @param code    Code de réinitialisation à envoyer
     */
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
        User user = userRepository.findByEmailIgnoreCase(toEmail).orElse(null);
        String greeting = buildGreeting(user);
        String textBody = "Voici votre code de réinitialisation: " + code + "\n"
                + "Ce code expire dans 15 minutes.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml(greeting, content, null, null);
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
        User user = userRepository.findByEmailIgnoreCase(toEmail).orElse(null);
        String greeting = buildGreeting(user);
        String textBody = "Une tentative de connexion suspecte a été détectée sur votre compte.\n\n"
                + "IP: " + (ip != null ? ip : "Inconnue") + "\n"
                + "Navigateur/Appareil: " + (userAgent != null ? userAgent : "Inconnu") + "\n\n"
                + "Si ce n'était pas vous, veuillez sécuriser votre compte immédiatement.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml(greeting, content, null, null);
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
        User user = userRepository.findByEmailIgnoreCase(newEmail).orElse(null);
        String greeting = buildGreeting(user);
        String textBody = "L'adresse email associée à votre compte a été modifiée.\n\n"
                + "Ancienne adresse: " + oldEmail + "\n"
                + "Nouvelle adresse: " + newEmail + "\n\n"
                + "Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement.\n\n"
                + "Cordialement,\n"
                + "L'équipe PneuMali";

        String htmlBody = buildEmailHtml(greeting, content, null, null);
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

        String greeting = buildGreeting(influenceur.getUser());
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

        String textBody = "Félicitations ! Un nouveau code promo vient de vous être attribué dans le cadre du programme d'affiliation PneuMali.\n\n"
                + "Code promo : " + promotion.getCode() + "\n"
                + reductionLabel + " : " + reductionValue + "\n"
                + "Validité : " + validityValue + "\n\n"
                + "Vous pouvez partager ce code avec votre communauté.\n"
                + "Chaque achat effectué avec votre code sera automatiquement relié à votre compte, et vos commissions seront calculées en conséquence.\n\n"
                + "Suivez les performances de votre code depuis votre espace influenceur.\n\n"
                + "Merci de faire partie de la communauté PneuMali 🙌\n"
                + "Support : " + supportEmail;

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
        String greeting = "";
        String baseUrl = normalizeFrontendUrl();
        String resetLink = baseUrl + "/auth/set-password?token=" + resetToken + "&email=" + toEmail;

        String content = "Bienvenue dans l'équipe PneuMali en tant qu'influenceur !<br><br>"
                + "Votre compte a été créé avec succès. Pour commencer, vous devez définir votre mot de passe.<br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Ce lien est valide pendant 7 jours.</span><br><br>"
                + "<span style=\"color: #666666; font-size: 14px;\">Si vous n'avez pas demandé la création de ce compte, vous pouvez ignorer cet email.</span>";
        String textBody = "Bienvenue dans l'équipe PneuMali en tant qu'influenceur !\n\n"
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

    @Async
    public void sendQuoteRequestConfirmation(User user, QuoteRequest request) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String subject = "PneuMali - Demande de devis " + request.getRequestNumber();
        String greeting = buildGreeting(user);
        String content = """
                Nous avons bien reçu votre demande de devis. Notre équipe va l'analyser et vous fera parvenir une proposition détaillée dans les meilleurs délais.<br><br>
                <strong>Numéro de demande :</strong> %s<br>
                Date : %s<br><br>
                Vous recevrez un email dès que le devis sera disponible dans votre espace client.
                """
                .formatted(request.getRequestNumber(),
                        request.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        sendHtmlEmailSafely(user.getEmail(), subject, buildEmailHtml(greeting, content, null, null), content,
                "confirmation devis");
    }

    @Async
    public void notifyAdminsNewQuoteRequest(QuoteRequest request) {
        if (adminEmails == null || adminEmails.isBlank()) {
            return;
        }
        String subject = "Nouvelle demande de devis " + request.getRequestNumber();
        String body = """
                Une nouvelle demande de devis vient d'être soumise sur PneuMali.

                Numéro : %s
                Client : %s %s (%s)
                Date : %s

                Rendez-vous dans le backoffice pour préparer le devis.
                """.formatted(request.getRequestNumber(),
                safe(request.getUser().getFirstName()),
                safe(request.getUser().getLastName()),
                request.getUser().getEmail(),
                request.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        for (String email : adminEmails.split(",")) {
            String trimmed = email.trim();
            if (!trimmed.isEmpty()) {
                sendEmailSafely(trimmed, subject, body, "notification devis");
            }
        }
    }

    @Async
    public void sendQuoteReadyEmail(User user, QuoteRequest request, String frontendQuoteUrl) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String baseUrl = normalizeFrontendUrl();
        String link = frontendQuoteUrl != null && !frontendQuoteUrl.isBlank()
                ? frontendQuoteUrl
                : baseUrl + "/devis/" + request.getRequestNumber();
        String subject = "Votre devis " + request.getQuoteNumber() + " est disponible";
        String greeting = buildGreeting(user);
        String content = """
                Votre devis est maintenant prêt et disponible dans votre espace client.<br><br>
                <strong>Numéro de devis :</strong> %s<br>
                Total devis : %s<br>
                Validité : %s<br><br>
                Veuillez consulter et valider le devis afin de confirmer votre commande.
                """.formatted(
                request.getQuoteNumber(),
                formatCurrencyXof(request.getTotalQuoted()),
                request.getValidUntil() != null
                        ? request.getValidUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "7 jours");

        String textBody = """
                %s

                Votre devis est disponible.

                Numéro : %s
                Total : %s
                Validité : %s

                Consulter le devis : %s

                Merci pour votre confiance.
                """.formatted(
                greeting,
                request.getQuoteNumber(),
                formatCurrencyXof(request.getTotalQuoted()),
                request.getValidUntil() != null
                        ? request.getValidUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "7 jours",
                link);

        String htmlBody = buildEmailHtml(greeting, content, "Consulter le devis", link);
        sendHtmlEmailSafely(user.getEmail(), subject, htmlBody, textBody, "devis prêt");
    }

    @Async
    public void notifyAdminsQuoteValidated(QuoteRequest request) {
        if (adminEmails == null || adminEmails.isBlank()) {
            return;
        }
        String subject = "Devis validé par le client - " + request.getQuoteNumber();
        String body = """
                Le devis %s vient d'être validé électroniquement par le client %s %s.

                Montant : %s
                Date : %s
                IP : %s

                Merci de lancer la préparation et la livraison.
                """.formatted(
                request.getQuoteNumber(),
                safe(request.getUser().getFirstName()),
                safe(request.getUser().getLastName()),
                formatCurrencyXof(request.getTotalQuoted()),
                request.getValidatedAt() != null
                        ? request.getValidatedAt().toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "-",
                request.getValidatedIp() != null ? request.getValidatedIp() : "-");

        for (String email : adminEmails.split(",")) {
            String trimmed = email.trim();
            if (!trimmed.isEmpty()) {
                sendEmailSafely(trimmed, subject, body, "validation devis");
            }
        }
    }

    @Async
    public void notifyLivreurAssignment(User livreur, QuoteRequest request) {
        notifyLivreurAssignmentSync(livreur, request);
    }

    /**
     * Version synchrone de notifyLivreurAssignment pour permettre la gestion des
     * erreurs.
     * Retourne true si l'email a été envoyé avec succès, false sinon.
     * 
     * @param livreur Livreur à qui envoyer l'email
     * @param request Devis assigné
     * @return true si l'email a été envoyé avec succès, false sinon
     */
    public boolean notifyLivreurAssignmentSync(User livreur, QuoteRequest request) {
        if (livreur == null || livreur.getEmail() == null || livreur.getEmail().isBlank()) {
            log.warn("Impossible d'envoyer l'email d'assignation: livreur ou email manquant");
            return false;
        }
        if (request == null || request.getUser() == null) {
            log.warn("Impossible d'envoyer l'email d'assignation: devis ou client manquant");
            return false;
        }

        String subject = "Nouvelle livraison assignée - Devis " + safe(request.getQuoteNumber());
        String greeting = buildGreeting(livreur);
        String content = buildLivreurAssignmentContent(request);

        return sendHtmlEmailSafelySync(livreur.getEmail(), subject, buildEmailHtml(greeting, content, null, null),
                buildLivreurAssignmentTextContent(request), "assignation livreur");
    }

    private String buildLivreurAssignmentContent(QuoteRequest request) {
        User client = request.getUser();
        StringBuilder content = new StringBuilder();

        content.append("<p style=\"font-size: 16px; line-height: 1.6; color: #333333;\">");
        content.append(
                "Une nouvelle livraison vous a été assignée. Veuillez trouver ci-dessous tous les détails nécessaires pour effectuer la livraison.");
        content.append("</p><br>");

        // Informations du devis
        content.append(
                "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;\">");
        content.append("<h3 style=\"margin-top: 0; color: #00d9ff; font-size: 18px;\">📋 Informations du devis</h3>");
        content.append("<p style=\"margin: 5px 0;\"><strong>Numéro de devis :</strong> ")
                .append(safe(request.getQuoteNumber())).append("</p>");
        if (request.getTotalQuoted() != null) {
            content.append("<p style=\"margin: 5px 0;\"><strong>Montant total :</strong> ")
                    .append(formatCurrency(request.getTotalQuoted())).append(" FCFA</p>");
        }
        content.append("</div>");

        // Informations du client
        content.append(
                "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;\">");
        content.append("<h3 style=\"margin-top: 0; color: #00d9ff; font-size: 18px;\">👤 Informations du client</h3>");
        String clientName = buildClientFullName(client);
        content.append("<p style=\"margin: 5px 0;\"><strong>Nom :</strong> ").append(clientName).append("</p>");
        content.append("<p style=\"margin: 5px 0;\"><strong>Email :</strong> ").append(safe(client.getEmail()))
                .append("</p>");
        if (client.getPhoneNumber() != null && !client.getPhoneNumber().isBlank()) {
            content.append("<p style=\"margin: 5px 0;\"><strong>Téléphone :</strong> ")
                    .append(safe(client.getPhoneNumber())).append("</p>");
        }
        content.append("</div>");

        // Adresse de livraison
        String deliveryAddress = buildDeliveryAddress(request);
        if (!deliveryAddress.isBlank()) {
            content.append(
                    "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;\">");
            content.append(
                    "<h3 style=\"margin-top: 0; color: #00d9ff; font-size: 18px;\">📍 Adresse de livraison</h3>");
            content.append("<p style=\"margin: 5px 0; white-space: pre-line;\">").append(deliveryAddress)
                    .append("</p>");
            content.append("</div>");
        }

        // Articles à livrer
        String itemsList = buildItemsList(request);
        if (!itemsList.isBlank()) {
            content.append(
                    "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;\">");
            content.append("<h3 style=\"margin-top: 0; color: #00d9ff; font-size: 18px;\">📦 Articles à livrer</h3>");
            content.append(itemsList);
            content.append("</div>");
        }

        // Instructions de livraison
        if (request.getDeliveryDetails() != null && !request.getDeliveryDetails().isBlank()) {
            content.append(
                    "<div style=\"background-color: #fff3cd; padding: 15px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #ffc107;\">");
            content.append(
                    "<h3 style=\"margin-top: 0; color: #856404; font-size: 18px;\">⚠️ Instructions de livraison</h3>");
            content.append("<p style=\"margin: 5px 0; white-space: pre-line; color: #856404;\">")
                    .append(request.getDeliveryDetails().replace("\n", "<br>")).append("</p>");
            content.append("</div>");
        }

        // Notes administratives (si pertinentes)
        if (request.getAdminNotes() != null && !request.getAdminNotes().isBlank()) {
            content.append(
                    "<div style=\"background-color: #e7f3ff; padding: 15px; border-radius: 8px; margin-bottom: 20px;\">");
            content.append("<h3 style=\"margin-top: 0; color: #004085; font-size: 18px;\">ℹ️ Notes internes</h3>");
            content.append("<p style=\"margin: 5px 0; white-space: pre-line; color: #004085;\">")
                    .append(request.getAdminNotes().replace("\n", "<br>")).append("</p>");
            content.append("</div>");
        }

        content.append("<p style=\"font-size: 14px; color: #666666; margin-top: 20px;\">");
        content.append(
                "Merci de confirmer la réception de cette assignation et de procéder à la livraison dans les meilleurs délais.");
        content.append("</p>");

        return content.toString();
    }

    private String buildLivreurAssignmentTextContent(QuoteRequest request) {
        User client = request.getUser();
        StringBuilder text = new StringBuilder();

        text.append("NOUVELLE LIVRAISON ASSIGNÉE\n\n");
        text.append("Une nouvelle livraison vous a été assignée.\n\n");

        text.append("INFORMATIONS DU DEVIS\n");
        text.append("Numéro de devis: ").append(safe(request.getQuoteNumber())).append("\n");
        if (request.getTotalQuoted() != null) {
            text.append("Montant total: ").append(formatCurrency(request.getTotalQuoted())).append(" FCFA\n");
        }
        text.append("\n");

        text.append("INFORMATIONS DU CLIENT\n");
        text.append("Nom: ").append(buildClientFullName(client)).append("\n");
        text.append("Email: ").append(safe(client.getEmail())).append("\n");
        if (client.getPhoneNumber() != null && !client.getPhoneNumber().isBlank()) {
            text.append("Téléphone: ").append(safe(client.getPhoneNumber())).append("\n");
        }
        text.append("\n");

        String deliveryAddress = buildDeliveryAddress(request);
        if (!deliveryAddress.isBlank()) {
            text.append("ADRESSE DE LIVRAISON\n").append(deliveryAddress).append("\n\n");
        }

        String itemsList = buildItemsListText(request);
        if (!itemsList.isBlank()) {
            text.append("ARTICLES À LIVRER\n").append(itemsList).append("\n");
        }

        if (request.getDeliveryDetails() != null && !request.getDeliveryDetails().isBlank()) {
            text.append("INSTRUCTIONS DE LIVRAISON\n").append(request.getDeliveryDetails()).append("\n\n");
        }

        text.append("Merci de confirmer la réception et de procéder à la livraison.\n");

        return text.toString();
    }

    private String buildClientFullName(User client) {
        String firstName = safe(client.getFirstName());
        String lastName = safe(client.getLastName());
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? safe(client.getEmail()) : fullName;
    }

    private String buildDeliveryAddress(QuoteRequest request) {
        User client = request.getUser();
        if (client == null || client.getAddresses() == null || client.getAddresses().isEmpty()) {
            return "";
        }

        Address defaultAddress = client.getAddresses().stream()
                .filter(addr -> addr != null && addr.isDefault())
                .findFirst()
                .orElse(client.getAddresses().stream()
                        .filter(addr -> addr != null)
                        .findFirst()
                        .orElse(null));

        if (defaultAddress == null) {
            return "";
        }

        StringBuilder address = new StringBuilder();
        if (defaultAddress.getStreet() != null && !defaultAddress.getStreet().isBlank()) {
            address.append(defaultAddress.getStreet());
        }
        if (defaultAddress.getCity() != null && !defaultAddress.getCity().isBlank()) {
            appendAddressPart(address, defaultAddress.getCity());
        }
        if (defaultAddress.getRegion() != null && !defaultAddress.getRegion().isBlank()) {
            appendAddressPart(address, defaultAddress.getRegion());
        }
        if (defaultAddress.getPostalCode() != null && !defaultAddress.getPostalCode().isBlank()) {
            appendAddressPart(address, defaultAddress.getPostalCode());
        }
        if (defaultAddress.getCountry() != null) {
            appendAddressPart(address, defaultAddress.getCountry().getDisplayName());
        }
        if (defaultAddress.getPhoneNumber() != null && !defaultAddress.getPhoneNumber().isBlank()) {
            address.append("<br><strong>Téléphone de livraison :</strong> ").append(defaultAddress.getPhoneNumber());
        }

        return address.toString().trim();
    }

    private void appendAddressPart(StringBuilder builder, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private String buildItemsList(QuoteRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return "";
        }

        StringBuilder items = new StringBuilder();
        items.append("<table style=\"width: 100%; border-collapse: collapse; margin-top: 10px;\">");
        items.append("<thead>");
        items.append("<tr style=\"background-color: #e9ecef;\">");
        items.append("<th style=\"padding: 10px; text-align: left; border: 1px solid #dee2e6;\">Produit</th>");
        items.append("<th style=\"padding: 10px; text-align: center; border: 1px solid #dee2e6;\">Quantité</th>");
        items.append("<th style=\"padding: 10px; text-align: right; border: 1px solid #dee2e6;\">Prix unitaire</th>");
        items.append("</tr>");
        items.append("</thead>");
        items.append("<tbody>");

        for (QuoteRequestItem item : request.getItems()) {
            items.append("<tr>");
            String productName = buildProductName(item);
            items.append("<td style=\"padding: 10px; border: 1px solid #dee2e6;\">").append(productName)
                    .append("</td>");
            items.append("<td style=\"padding: 10px; text-align: center; border: 1px solid #dee2e6;\">")
                    .append(item.getQuantity()).append("</td>");
            items.append("<td style=\"padding: 10px; text-align: right; border: 1px solid #dee2e6;\">")
                    .append(formatCurrency(item.getUnitPrice())).append(" FCFA</td>");
            items.append("</tr>");
        }

        items.append("</tbody>");
        items.append("</table>");

        return items.toString();
    }

    private String buildItemsListText(QuoteRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return "";
        }

        StringBuilder items = new StringBuilder();
        for (QuoteRequestItem item : request.getItems()) {
            items.append("- ").append(buildProductName(item))
                    .append(" (Quantité: ").append(item.getQuantity())
                    .append(", Prix unitaire: ").append(formatCurrency(item.getUnitPrice()))
                    .append(" FCFA)\n");
        }
        return items.toString();
    }

    private String buildProductName(QuoteRequestItem item) {
        StringBuilder name = new StringBuilder();
        if (item.getBrandName() != null && !item.getBrandName().isBlank()) {
            name.append(item.getBrandName()).append(" ");
        }
        if (item.getProductName() != null && !item.getProductName().isBlank()) {
            name.append(item.getProductName());
        }
        if (item.getWidthValue() != null || item.getProfileValue() != null || item.getDiameterValue() != null) {
            name.append(" ");
            if (item.getWidthValue() != null) {
                name.append(item.getWidthValue());
            }
            if (item.getProfileValue() != null) {
                name.append("/").append(item.getProfileValue());
            }
            if (item.getDiameterValue() != null) {
                name.append(" R").append(item.getDiameterValue());
            }
        }
        return name.toString().trim();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return String.format("%,.0f", amount.doubleValue()).replace(",", " ");
    }

    @Async
    public void notifyQuoteDelivered(QuoteRequest request) {
        if (request.getUser() == null || request.getUser().getEmail() == null
                || request.getUser().getEmail().isBlank()) {
            return;
        }
        String quoteNumber = request.getQuoteNumber() != null ? request.getQuoteNumber() : request.getRequestNumber();
        String subject = "Livraison effectuée - Devis " + quoteNumber;
        String greeting = buildGreeting(request.getUser());
        String content = """
                <p>Votre commande a été marquée comme livrée par le livreur.</p>
                <p>Merci de confirmer la réception en accédant à votre espace client.</p>
                """;
        String linkText = "Confirmer la livraison";
        String linkUrl = frontendUrl + "/mon-compte/devis";
        sendHtmlEmailSafely(request.getUser().getEmail(), subject,
                buildEmailHtml(greeting, content, linkText, linkUrl), content, "livraison effectuée");
    }

    @Async
    public void notifyClientAbsent(QuoteRequest request) {
        if (request.getUser() == null || request.getUser().getEmail() == null
                || request.getUser().getEmail().isBlank()) {
            return;
        }
        String quoteNumber = request.getQuoteNumber() != null ? request.getQuoteNumber() : request.getRequestNumber();
        String subject = "Client absent - Devis " + quoteNumber;
        String greeting = buildGreeting(request.getUser());
        String content = """
                <p>Le livreur s'est présenté à votre adresse mais n'a pas pu vous joindre.</p>
                <p>Veuillez programmer une nouvelle date de livraison.</p>
                """;
        String linkText = "Gérer ma livraison";
        String linkUrl = frontendUrl + "/mon-compte/devis";
        sendHtmlEmailSafely(request.getUser().getEmail(), subject,
                buildEmailHtml(greeting, content, linkText, linkUrl), content, "client absent");
    }

    @Async
    public void notifyClientMultipleAbsences(QuoteRequest request) {
        if (request.getUser() == null || request.getUser().getEmail() == null
                || request.getUser().getEmail().isBlank()) {
            return;
        }
        String quoteNumber = request.getQuoteNumber() != null ? request.getQuoteNumber() : request.getRequestNumber();
        String subject = "Commande renvoyée au dépôt - Devis " + quoteNumber;
        String greeting = buildGreeting(request.getUser());
        String content = """
                <p>Après deux tentatives de livraison infructueuses, votre commande a été renvoyée au dépôt.</p>
                <p>Un administrateur vous contactera prochainement pour définir la suite : retrait sur place ou nouvelle tentative de livraison.</p>
                """;
        sendHtmlEmailSafely(request.getUser().getEmail(), subject,
                buildEmailHtml(greeting, content, null, null), content, "renvoi au dépôt");
    }

    /**
     * Construit une salutation personnalisée pour l'utilisateur.
     * Retourne une chaîne vide car les salutations ne sont plus utilisées.
     * 
     * @param user Utilisateur pour lequel construire la salutation
     * @return Chaîne vide (salutations désactivées)
     */
    private String buildGreeting(User user) {
        return "";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
