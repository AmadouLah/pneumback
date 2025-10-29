package com.pneumaliback.www.service.mail;

/**
 * Interface pour l'envoi d'emails
 * Pattern Strategy : permet de basculer entre différentes implémentations
 * (SMTP classique, SendGrid API, etc.) sans modifier le code client
 */
public interface EmailSender {

    /**
     * Envoie un email simple
     * 
     * @param to      Destinataire
     * @param subject Sujet
     * @param body    Corps du message
     * @throws Exception En cas d'erreur d'envoi
     */
    void sendEmail(String to, String subject, String body) throws Exception;

    /**
     * Retourne le nom de l'implémentation pour les logs
     */
    String getProviderName();
}
