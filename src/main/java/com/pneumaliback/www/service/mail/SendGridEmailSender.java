package com.pneumaliback.www.service.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Implémentation SendGrid (pour Render et autres clouds bloquant SMTP)
 * Utilise l'API HTTP au lieu des ports SMTP
 * 
 * Avantages :
 * - Fonctionne sur Render (pas de port SMTP bloqué)
 * - Gratuit jusqu'à 100 emails/jour
 * - Fiable et rapide
 */
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailSender implements EmailSender {

    private final SendGrid sendGridClient;
    private final String fromAddress;

    @Override
    public void sendEmail(String to, String subject, String body) throws IOException {
        Email from = new Email(fromAddress, "PneuMali");
        Email toEmail = new Email(to);
        Email replyTo = new Email(fromAddress);

        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);
        mail.setReplyTo(replyTo);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        log.info("📤 Envoi via SendGrid: from={}, to={}", fromAddress, to);

        Response response = sendGridClient.api(request);
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody();

        if (statusCode >= 200 && statusCode < 300) {
            log.info("✅ Email envoyé à {} (status: {})", to, statusCode);
            if (statusCode == 202) {
                log.warn("⚠️  Email accepté mais peut arriver en SPAM sans SPF/DKIM configuré");
                log.warn("💡 Pour éviter les spams, configurez l'authentification de domaine:");
                log.warn("   → https://app.sendgrid.com/settings/sender_auth");
                log.warn("   → Authenticate Your Domain (recommandé)");
                log.warn("   → Cela configure SPF et DKIM automatiquement");
            }
        } else {
            String errorMsg = String.format("SendGrid erreur (status: %d) - %s",
                    statusCode, responseBody);
            log.error("❌ {}", errorMsg);
            throw new IOException(errorMsg);
        }
    }

    @Override
    public String getProviderName() {
        return "SendGrid";
    }
}
