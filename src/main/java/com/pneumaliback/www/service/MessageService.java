package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Message;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.MessageRepository;
import com.pneumaliback.www.repository.UserRepository;
import com.pneumaliback.www.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public Message send(Long authorId, Long recipientId, String content) {
        log.info("Message: {} -> {}", authorId, recipientId);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + authorId));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + recipientId));
        Message m = new Message();
        m.setAuthor(author);
        m.setRecipient(recipient);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now());
        Message saved = messageRepository.save(m);
        notificationService.notify(recipient, "Nouveau message", content);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Message> inbox(Long recipientId, Pageable pageable) {
        return messageRepository.findByRecipientIdOrderBySentAtDesc(recipientId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> outbox(Long authorId, Pageable pageable) {
        return messageRepository.findByAuthorIdOrderBySentAtDesc(authorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> conversation(Long utilisateur1, Long utilisateur2, Pageable pageable) {
        return messageRepository.conversation(utilisateur1, utilisateur2, pageable);
    }

    public int markConversationRead(Long recipientId, Long authorId) {
        return messageRepository.marquerConversationLue(recipientId, authorId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> unreadByInterlocutor(Long recipientId) {
        List<MessageRepository.UnreadCountProjection> rows = messageRepository.nonLusParInterlocuteur(recipientId);
        return rows.stream().collect(Collectors.toMap(MessageRepository.UnreadCountProjection::getInterlocuteurId,
                MessageRepository.UnreadCountProjection::getTotal));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> threadList(Long userId, String query, int limit) {
        Map<Long, Long> unread = unreadByInterlocutor(userId);
        return messageRepository.threads(userId, query).stream()
                .limit(limit)
                .map(t -> {
                    Long interlocuteurId = t.getInterlocuteurId();
                    Page<Message> last = messageRepository.conversation(userId, interlocuteurId,
                            org.springframework.data.domain.PageRequest.of(0, 1));
                    String content = last.hasContent() ? last.getContent().get(0).getContent() : "";
                    String apercu = content != null ? content.replace('\n', ' ').replace('\r', ' ').trim() : "";
                    if (apercu.length() > 120)
                        apercu = apercu.substring(0, 119) + "\u2026";
                    var userOpt = userRepository.findById(interlocuteurId);
                    String lastName = userOpt.map(u -> u.getLastName()).orElse("");
                    String firstName = userOpt.map(u -> u.getFirstName()).orElse("");

                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("interlocuteurId", interlocuteurId);
                    m.put("lastName", lastName);
                    m.put("firstName", firstName);
                    m.put("lastDate", t.getLastDate());
                    m.put("apercu", apercu);
                    m.put("nonLus", unread.getOrDefault(interlocuteurId, 0L));
                    return m;
                })
                .collect(Collectors.toList());
    }
}
