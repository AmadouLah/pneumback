package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByAuthorIdOrderBySentAtDesc(Long auteurId, Pageable pageable);

    Page<Message> findByRecipientIdOrderBySentAtDesc(Long destinataireId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long destinataireId);

    @Query("SELECT m FROM Message m WHERE (m.author.id = :u1 AND m.recipient.id = :u2) OR (m.author.id = :u2 AND m.recipient.id = :u1) ORDER BY m.sentAt DESC")
    Page<Message> conversation(@Param("u1") Long utilisateur1, @Param("u2") Long utilisateur2, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.author.id = :auteurId AND m.recipient.id = :destinataireId AND m.isRead = false")
    int marquerConversationLue(@Param("destinataireId") Long destinataireId, @Param("auteurId") Long auteurId);

    interface UnreadCountProjection {
        Long getInterlocuteurId();

        Long getTotal();
    }

    @Query("SELECT m.author.id AS interlocuteurId, COUNT(m) AS total FROM Message m WHERE m.recipient.id = :destinataireId AND m.isRead = false GROUP BY m.author.id")
    java.util.List<UnreadCountProjection> nonLusParInterlocuteur(@Param("destinataireId") Long destinataireId);

    interface ThreadHeadProjection {
        Long getInterlocuteurId();

        java.time.LocalDateTime getLastDate();
    }

    @Query("SELECT CASE WHEN m.author.id = :userId THEN m.recipient.id ELSE m.author.id END AS interlocuteurId, MAX(m.sentAt) AS lastDate "
            +
            "FROM Message m WHERE (m.author.id = :userId OR m.recipient.id = :userId) " +
            "AND (:q IS NULL OR :q = '' OR " +
            "     (CASE WHEN m.author.id = :userId THEN LOWER(m.recipient.lastName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "           ELSE LOWER(m.author.lastName) LIKE LOWER(CONCAT('%', :q, '%')) END) OR " +
            "     (CASE WHEN m.author.id = :userId THEN LOWER(m.recipient.firstName) LIKE LOWER(CONCAT('%', :q, '%')) "
            +
            "           ELSE LOWER(m.author.firstName) LIKE LOWER(CONCAT('%', :q, '%')) END)) " +
            "GROUP BY CASE WHEN m.author.id = :userId THEN m.recipient.id ELSE m.author.id END " +
            "ORDER BY lastDate DESC")
    java.util.List<ThreadHeadProjection> threads(@Param("userId") Long userId, @Param("q") String query);
}
