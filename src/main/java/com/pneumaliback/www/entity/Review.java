package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "reviews")
@Data
@EqualsAndHashCode(callSuper = true)
public class Review extends EntiteAuditable {

    @Column(nullable = false)
    private int rating; // 1 à 5 étoiles

    @Column(length = 1000)
    private String comment;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Product product;
}
