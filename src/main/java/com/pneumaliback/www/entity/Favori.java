package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "favoris")
@Data
@EqualsAndHashCode(callSuper = true)
public class Favori extends EntiteAuditable {

    @Column(length = 500)
    private String personalComment;

    @Column(length = 255)
    private String tags;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Product product;
}
