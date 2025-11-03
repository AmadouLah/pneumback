package com.pneumaliback.www.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "vehicle_types")
@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleType extends EntiteAuditable {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(optional = false)
    private Category category;

    @Column(nullable = false)
    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "vehicleType")
    private List<Product> products = new ArrayList<>();
}
