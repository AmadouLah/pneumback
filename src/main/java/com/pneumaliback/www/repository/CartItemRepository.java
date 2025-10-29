package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.CartItem;
import com.pneumaliback.www.entity.Cart;
import com.pneumaliback.www.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart(Cart cart);
    List<CartItem> findByCartId(Long cartId);
    
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    
    boolean existsByCartAndProduct(Cart cart, Product product);
    boolean existsByCartIdAndProductId(Long cartId, Long productId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
    
    @Modifying
    @Query("UPDATE CartItem ci SET ci.quantity = :quantity WHERE ci.id = :itemId")
    void updateQuantity(@Param("itemId") Long itemId, @Param("quantity") int quantity);
}