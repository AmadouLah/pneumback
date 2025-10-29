package com.pneumaliback.www.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pneumaliback.www.enums.Country;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "addresses")
@Data
@EqualsAndHashCode(callSuper = true)

public class Address extends EntiteAuditable {

    @Column(nullable = false, length = 150)
    private String street;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country = Country.MALI;

    @Column(length = 50)
    private String postalCode;

    @Column(length = 20)
    private String phoneNumber;

    @JsonProperty("default")
    @Column(nullable = false)
    private boolean isDefault = false;

    @JsonIgnore
    @ManyToOne(optional = false)
    private User user;
}
