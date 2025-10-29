package com.pneumaliback.www.configuration;

import com.pneumaliback.www.service.mail.EmailSender;
import com.pneumaliback.www.service.mail.SendGridEmailSender;
import com.pneumaliback.www.service.mail.SmtpEmailSender;
import com.sendgrid.SendGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration intelligente du service email avec Pattern Strategy
 * 
 * Deux implémentations disponibles :
 * 1. SMTP classique (développement local, serveurs avec ports SMTP ouverts)
 * 2. SendGrid API HTTP (Render, Heroku, et autres clouds bloquant SMTP)
 * 
 * Configuration via propriétés :
 * - app.mail.provider=smtp → Utilise SMTP classique
 * - app.mail.provider=sendgrid → Utilise SendGrid (recommandé pour Render)
 */
@Configuration
@Slf4j
public class MailConfig {

    @Value("${app.mail.from:noreply@pneumali.ml}")
    private String fromAddress;

    /**
     * Bean EmailSender pour SendGrid (API HTTP)
     * Activé quand app.mail.provider=sendgrid
     */
    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "sendgrid")
    public EmailSender sendGridEmailSender(
            @Value("${app.mail.sendgrid.api-key}") String apiKey) {

        SendGrid sendGridClient = new SendGrid(apiKey);
        log.info("✅ Configuration email : SendGrid (API HTTP)");
        return new SendGridEmailSender(sendGridClient, fromAddress);
    }

    /**
     * Bean EmailSender pour SMTP classique
     * Activé quand app.mail.provider=smtp (défaut)
     */
    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
    public EmailSender smtpEmailSender(
            @Value("${spring.mail.host:smtp.gmail.com}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        log.info("✅ Configuration email : SMTP ({}:{})", host, port);
        return new SmtpEmailSender(mailSender, fromAddress);
    }
}
