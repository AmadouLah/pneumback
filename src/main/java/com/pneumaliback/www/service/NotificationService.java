package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.NotificationRechercheDTO;
import com.pneumaliback.www.entity.Notification;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.NotificationRepository;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification notify(User recipient, String title, String content) {
        return notify(recipient, title, content, "SYSTEME");
    }

    @Transactional
    public Notification notify(User recipient, String title, String content, String type) {
        log.info("Notification {} pour utilisateur {}: {}", type, recipient.getId(), title);
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);
        notification = notificationRepository.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), notification);
        return notification;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> unread(Long userId, Pageable pageable) {
        return notificationRepository.findNonLues(userId, pageable);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.marquerToutesCommeLues(userId);
    }

    @Transactional
    public int markAsRead(Long notificationId, Long userId) {
        return notificationRepository.marquerCommeLue(notificationId, userId);
    }

    @Transactional(readOnly = true)
    public Page<Notification> search(NotificationRechercheDTO criteres, Pageable pageable) {
        if (criteres.getQuery() != null && !criteres.getQuery().trim().isEmpty()) {
            return notificationRepository.rechercher(criteres.getUserId(), criteres.getQuery(), pageable);
        }
        if (criteres.getType() != null) {
            return notificationRepository.findByType(criteres.getUserId(), criteres.getType(), pageable);
        }
        if (criteres.getStartDate() != null && criteres.getEndDate() != null) {
            return notificationRepository.findByDateRange(criteres.getUserId(), criteres.getStartDate(),
                    criteres.getEndDate(), pageable);
        }
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(criteres.getUserId(), pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public long countUnreadByType(Long userId, String type) {
        return notificationRepository.countNonLuesParType(userId, type);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statisticsByType(Long userId) {
        List<Object[]> results = notificationRepository.statistiquesParType(userId);
        return results.stream().collect(java.util.stream.Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]));
    }

    @Transactional
    public int cleanupOld(Long userId, int retentionDays) {
        LocalDateTime dateLimite = LocalDateTime.now().minusDays(retentionDays);
        return notificationRepository.supprimerAnciennes(userId, dateLimite);
    }

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    // ===== MÉTHODES WEBSOCKET INTÉGRÉES =====

    /**
     * Envoie le count des notifications non lues via WebSocket
     */
    public void sendUnreadCount(User user) {
        String destination = "/topic/notifications/" + user.getId() + "/count";
        long count = countUnread(user.getId());

        log.debug("Sending unread notifications count via WebSocket to user {}: {}", user.getId(), count);
        messagingTemplate.convertAndSend(destination, Map.of("count", count));
    }

    /**
     * Notifie un nouveau message reçu (base + WebSocket)
     */
    public void notifyNewMessage(User recipient, String senderName, Long messageId) {
        // Créer notification en base
        notify(recipient, "Nouveau message",
                "Vous avez reçu un nouveau message de " + senderName, "NOUVEAU_MESSAGE");

        // Envoyer message WebSocket spécifique
        String destination = "/topic/notifications/" + recipient.getId();
        NotificationMessage message = new NotificationMessage(
                "Nouveau message",
                "Vous avez reçu un nouveau message de " + senderName,
                "NOUVEAU_MESSAGE",
                messageId);

        log.info("Notification nouveau message WebSocket à l'utilisateur {}: {}", recipient.getId(), senderName);
        messagingTemplate.convertAndSend(destination, message);

        // Mettre à jour le count
        sendUnreadCount(recipient);
    }

    /**
     * Notifie l'assignation d'un colis (base + WebSocket)
     */
    public void notifyPackageAssigned(User recipient, String packageDescription, Long packageId) {
        // Créer notification en base
        notify(recipient, "Colis assigné",
                "Un colis vous a été assigné: " + packageDescription, "COLIS_AFFECTE");

        // Envoyer message WebSocket spécifique
        String destination = "/topic/notifications/" + recipient.getId();
        NotificationMessage message = new NotificationMessage(
                "Colis assigné",
                "Un colis vous a été assigné: " + packageDescription,
                "COLIS_AFFECTE",
                packageId);

        log.info("Notification colis assigné WebSocket à l'utilisateur {}: {}", recipient.getId(), packageDescription);
        messagingTemplate.convertAndSend(destination, message);

        // Mettre à jour le count
        sendUnreadCount(recipient);
    }

    /**
     * Notifie la confirmation d'un paiement (base + WebSocket)
     */
    public void notifyPaymentConfirmed(User recipient, String amount, Long paymentId) {
        // Créer notification en base
        notify(recipient, "Paiement confirmé",
                "Votre paiement de " + amount + " a été confirmé", "PAIEMENT_RECU");

        // Envoyer message WebSocket spécifique
        String destination = "/topic/notifications/" + recipient.getId();
        NotificationMessage message = new NotificationMessage(
                "Paiement confirmé",
                "Votre paiement de " + amount + " a été confirmé",
                "PAIEMENT_RECU",
                paymentId);

        log.info("Notification paiement confirmé WebSocket à l'utilisateur {}: {}", recipient.getId(), amount);
        messagingTemplate.convertAndSend(destination, message);

        // Mettre à jour le count
        sendUnreadCount(recipient);
    }

    /**
     * Classe interne pour les messages de notification WebSocket
     */
    public static class NotificationMessage {
        private final String title;
        private final String content;
        private final String type;
        private final Long referenceId;

        public NotificationMessage(String title, String content, String type, Long referenceId) {
            this.title = title;
            this.content = content;
            this.type = type;
            this.referenceId = referenceId;
        }

        // Getters
        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getType() {
            return type;
        }

        public Long getReferenceId() {
            return referenceId;
        }
    }
}
