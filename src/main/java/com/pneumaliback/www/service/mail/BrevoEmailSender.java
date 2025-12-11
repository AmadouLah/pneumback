package com.pneumaliback.www.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation Brevo (API HTTP moderne)
 * Documentation : https://developers.brevo.com/docs/getting-started
 * Avantages :
 * - API HTTP moderne
 * - Excellent pour envoi transactionnel
 * - Bonne délivrabilité
 * - 300 emails gratuits/jour
 * - Pas de dépendance Maven externe nécessaire
 */
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailSender implements EmailSender {

    private final String apiKey;
    private final String fromAddress;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        sendHtmlEmail(to, subject, "<pre>" + body.replace("\n", "<br>") + "</pre>", body);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody, String textBody) throws Exception {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> sender = new HashMap<>();
            sender.put("email", fromAddress);
            sender.put("name", "PneuMali");

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", to);

            Map<String, Object> emailPayload = new HashMap<>();
            emailPayload.put("sender", sender);
            emailPayload.put("to", new Object[] { recipient });
            emailPayload.put("subject", subject);
            emailPayload.put("htmlContent", htmlBody);
            emailPayload.put("textContent", textBody != null ? textBody : htmlBody.replaceAll("<[^>]+>", ""));
            
            // Sécurité : Aucune copie (CC/BCC) n'est envoyée pour garantir la confidentialité
            // Les codes de vérification ne doivent être envoyés qu'au destinataire unique
            // Ne pas ajouter de "cc" ou "bcc" dans le payload

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);

            log.info("📤 Envoi via Brevo: from={}, to={} (aucune copie)", fromAddress, to);
            @SuppressWarnings("rawtypes")
            ResponseEntity response = restTemplate.postForEntity(BREVO_API_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email envoyé à {} via Brevo (destinataire unique uniquement)", to);
            } else {
                throw new Exception("Erreur Brevo: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Erreur Brevo: {}", e.getMessage());
            throw new Exception("Erreur lors de l'envoi de l'email", e);
        }
    }

    @Override
    public String getProviderName() {
        return "Brevo";
    }
}
