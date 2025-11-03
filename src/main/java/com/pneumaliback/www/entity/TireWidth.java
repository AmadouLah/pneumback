package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "tire_widths")
@Data
@EqualsAndHashCode(callSuper = true)
public class TireWidth extends EntiteAuditable {
    @Column(nullable = false, unique = true)
    private Integer value;

    @Column(nullable = false)
    private boolean active = true;
}
