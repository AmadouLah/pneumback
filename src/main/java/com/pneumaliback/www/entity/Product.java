package com.pneumaliback.www.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pneumaliback.www.enums.TireSeason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = true)
public class Product extends EntiteAuditable {
    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @ManyToOne
    private Brand brand;

    @Column(length = 50)
    private String size;

    @ManyToOne
    private TireWidth width;

    @ManyToOne
    private TireProfile profile;

    @ManyToOne
    private TireDiameter diameter;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TireSeason season;

    @ManyToOne
    private VehicleType vehicleType;

    @ManyToOne
    private TireCondition tireCondition;

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(optional = false)
    private Category category;

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<Review> reviews = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<Favori> favoris = new ArrayList<>();
}
