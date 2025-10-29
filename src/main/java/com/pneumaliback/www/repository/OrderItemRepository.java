package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.OrderItem;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrderId(Long orderId);
    
    List<OrderItem> findByProduct(Product product);
    List<OrderItem> findByProductId(Long productId);
    
    // === Statistiques de vente par produit ===
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long getTotalQuantitySoldForProduct(@Param("productId") Long productId);
    
    @Query("SELECT SUM(oi.quantity * oi.unitPrice) FROM OrderItem oi WHERE oi.product.id = :productId")
    java.math.BigDecimal getTotalRevenueForProduct(@Param("productId") Long productId);
    
    // === Top produits vendus ===
    @Query("SELECT oi.product, SUM(oi.quantity) FROM OrderItem oi " +
           "GROUP BY oi.product ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findBestSellingProducts();
    
    @Query("SELECT oi.product, SUM(oi.quantity * oi.unitPrice) FROM OrderItem oi " +
           "GROUP BY oi.product ORDER BY SUM(oi.quantity * oi.unitPrice) DESC")
    List<Object[]> findTopRevenueProducts();
    
    // === Ventes par période ===
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.createdAt BETWEEN :startDate AND :endDate")
    List<OrderItem> findByDateRange(@Param("startDate") LocalDateTime startDate, 
    @Param("endDate") LocalDateTime endDate);
  
}