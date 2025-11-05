package com.pneumaliback.www.service.mail;

/**
 * Interface pour l'envoi d'emails
 * Pattern Strategy : permet de basculer entre différentes implémentations
 * (Brevo API, LogOnly) sans modifier le code client
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
     * Envoie un email avec contenu HTML
     * 
     * @param to       Destinataire
     * @param subject  Sujet
     * @param htmlBody Corps HTML du message
     * @param textBody Version texte du message (fallback)
     * @throws Exception En cas d'erreur d'envoi
     */
    default void sendHtmlEmail(String to, String subject, String htmlBody, String textBody) throws Exception {
        // Par défaut, utiliser la méthode sendEmail standard
        sendEmail(to, subject, textBody != null ? textBody : htmlBody);
    }

    /**
     * Retourne le nom de l'implémentation pour les logs
     */
    String getProviderName();
}
