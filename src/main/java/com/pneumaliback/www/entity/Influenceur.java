package com.pneumaliback.www.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "influenceurs")
@Data
@EqualsAndHashCode(callSuper = true)
public class Influenceur extends EntiteAuditable {

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(length = 50, unique = true)
    private String promoCode;

    @OneToOne
    private User user;
}
