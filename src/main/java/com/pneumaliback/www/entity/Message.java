package com.pneumaliback.www.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messages")
@Data
@EqualsAndHashCode(callSuper = true)
public class Message extends EntiteAuditable {
    @Column(name = "contenu", nullable = false, length = 1000)
    private String content;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private User recipient;

    @Column(name = "est_lu")
    private Boolean isRead = false;
}
