package com.pneumaliback.www.entity;

import java.math.BigDecimal;

import com.pneumaliback.www.enums.PaymentMethod;
import com.pneumaliback.www.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_tx_ref", columnList = "transactionReference")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Payment extends EntiteAuditable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 50)
    private String provider; // ex: OrangeMoney, Visa, Mastercard

    @Column(length = 100, unique = true)
    private String transactionReference;

    @OneToOne
    private Order order;
}
