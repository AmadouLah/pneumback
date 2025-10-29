package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Cart;
import com.pneumaliback.www.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
    Optional<Cart> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
    
    @Query("SELECT c FROM Cart c WHERE SIZE(c.items) > 0")
    List<Cart> findNonEmptyCarts();
    
    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :cutoffDate AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCarts(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(ci.quantity) FROM Cart c JOIN c.items ci WHERE c.user.id = :userId")
    int getTotalItemsInCart(@Param("userId") Long userId);
}