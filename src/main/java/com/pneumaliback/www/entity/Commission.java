package com.pneumaliback.www.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.pneumaliback.www.enums.CommissionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "commissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_commission_order", columnNames = {"order_id"})
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Commission extends EntiteAuditable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "influenceur_id", nullable = false)
    private Influenceur influenceur;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal baseAmount; // e.g., order total at confirmation

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal rate; // percentage at time of sale

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount; // computed baseAmount * rate / 100

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionStatus status = CommissionStatus.PENDING;

    private LocalDateTime paidAt; // null until paid
}
