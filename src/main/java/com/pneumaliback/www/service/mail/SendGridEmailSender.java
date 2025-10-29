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

        Response response = sendGridClient.api(request);

        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            log.debug("Email envoyé via SendGrid à {} (status: {})", to, response.getStatusCode());
        } else {
            throw new IOException("Erreur SendGrid: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    @Override
    public String getProviderName() {
        return "SendGrid";
    }
}
