package com.pneumaliback.www.entity;

import java.util.ArrayList;
import java.util.List;

import com.pneumaliback.www.enums.OrderStatus;

import java.math.BigDecimal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_promotion_id", columnList = "promotion_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Order extends EntiteAuditable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @ManyToOne(optional = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Delivery delivery;
    
    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;
}
