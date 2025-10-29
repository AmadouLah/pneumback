package com.pneumaliback.www.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "notifications")
@Data
@EqualsAndHashCode(callSuper = true)
public class Notification extends EntiteAuditable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 50)
    private String type;

    @NotNull
    @Column(name = "est_lu", nullable = false)
    private Boolean isRead = false;
}
