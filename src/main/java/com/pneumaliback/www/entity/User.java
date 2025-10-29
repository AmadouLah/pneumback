package com.pneumaliback.www.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import com.pneumaliback.www.validation.StrongPassword;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.pneumaliback.www.enums.Country;
import com.pneumaliback.www.enums.Role;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends EntiteAuditable implements UserDetails {

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    @StrongPassword
    private String password;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private Country country = Country.MALI;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean credentialsNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @Column
    private Instant lockTime;

    @Column(length = 120)
    private String verificationCode;

    @Column
    private Instant verificationExpiry;

    @Column
    private Instant verificationSentAt;

    @Column(length = 100)
    private String previousEmail; // Stocke l'ancien email lors du changement

    @Column
    private Integer otpAttempts;

    @Column
    private Instant otpLockedUntil;

    @Column
    private Integer otpResendCount;

    @Column(length = 10)
    private String resetCode;

    @Column
    private Instant resetExpiry;

    @Column
    private Instant resetSentAt;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @Column(length = 45)
    private String lastLoginIp;

    @Column(length = 255)
    private String lastLoginUserAgent;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Favori> favoris = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
