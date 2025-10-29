package com.pneumaliback.www.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "order_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends EntiteAuditable {

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(optional = false)
    private Order order;

    @ManyToOne(optional = false)
    private Product product;
}
