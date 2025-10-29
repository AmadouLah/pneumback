package com.pneumaliback.www.entity;

import com.pneumaliback.www.enums.DeliveryStatus;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_deliveries_zone", columnList = "zone")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Delivery extends EntiteAuditable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(length = 100)
    private String trackingNumber;

    @Column(length = 100)
    private String zone;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @OneToOne
    private Order order;

    @ManyToOne
    private Address address;
}
