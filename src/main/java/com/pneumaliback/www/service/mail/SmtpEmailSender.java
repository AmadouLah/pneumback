package com.pneumaliback.www.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Implémentation SMTP classique (pour développement local ou serveurs avec SMTP
 * ouvert)
 */
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.debug("Email envoyé via SMTP à {}", to);
    }

    @Override
    public String getProviderName() {
        return "SMTP";
    }
}
