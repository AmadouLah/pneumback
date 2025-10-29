package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "cart_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class CartItem extends EntiteAuditable {

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(optional = false)
    private Cart cart;

    @ManyToOne(optional = false)
    private Product product;
}
