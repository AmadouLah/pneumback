package com.pneumaliback.www.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "tire_profiles")
@Data
@EqualsAndHashCode(callSuper = true)
public class TireProfile extends EntiteAuditable {
    @Column(nullable = false, unique = true)
    private Integer value;

    @Column(nullable = false)
    private boolean active = true;
}
