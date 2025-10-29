package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long utilisateurId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long utilisateurId);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    Page<Notification> findNonLues(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    int marquerToutesCommeLues(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.recipient.id = :userId")
    int marquerCommeLue(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByType(@Param("userId") Long userId, @Param("type") String type, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.createdAt BETWEEN :debut AND :fin ORDER BY n.createdAt DESC")
    Page<Notification> findByDateRange(@Param("userId") Long userId, @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY n.createdAt DESC")
    Page<Notification> rechercher(@Param("userId") Long userId, @Param("q") String query, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :userId AND n.type = :type AND n.isRead = false")
    long countNonLuesParType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.recipient.id = :userId GROUP BY n.type")
    List<Object[]> statistiquesParType(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :userId AND n.createdAt < :dateLimite")
    int supprimerAnciennes(@Param("userId") Long userId, @Param("dateLimite") LocalDateTime dateLimite);
}
