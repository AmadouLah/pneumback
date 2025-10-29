package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.RefreshToken;
import com.pneumaliback.www.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUser(User user);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteByUser(@Param("user") User user);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteByExpiryDateBefore(@Param("now") Instant now);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeByUser(@Param("user") User user);
}
