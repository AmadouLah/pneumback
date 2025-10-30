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
        Email from = new Email(fromAddress);
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        log.info("📤 Tentative d'envoi via SendGrid: from={}, to={}", fromAddress, to);

        Response response = sendGridClient.api(request);
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody();

        log.info("📨 Réponse SendGrid: status={}, body={}", statusCode,
                responseBody != null && !responseBody.isEmpty() ? responseBody : "empty");

        if (statusCode >= 200 && statusCode < 300) {
            log.info("✅ Email RÉELLEMENT envoyé via SendGrid à {} (status: {})", to, statusCode);
        } else {
            String errorMsg = String.format("SendGrid a refusé l'envoi (status: %d) - Réponse: %s",
                    statusCode, responseBody);
            log.error("❌ {}", errorMsg);
            log.error("💡 Vérifiez dans SendGrid Dashboard:");
            log.error("   1. L'adresse '{}' est-elle vérifiée ? (Settings → Sender Authentication)", fromAddress);
            log.error("   2. La clé API a-t-elle les permissions 'Mail Send' ?");
            log.error("   3. Consultez Activity pour voir les emails bloqués");
            throw new IOException(errorMsg);
        }
    }

    @Override
    public String getProviderName() {
        return "SendGrid";
    }
}
