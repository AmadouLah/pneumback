package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.enums.TireSeason;
import com.pneumaliback.www.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // === Actifs ===
    Page<Product> findByActiveTrue(Pageable pageable);
    List<Product> findByCategoryAndActiveTrue(Category category);

    // === Recherche de base ===
    List<Product> findByNameContainingIgnoreCase(String name);
    Optional<Product> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    
    // === Recherche par catégorie ===
    List<Product> findByCategory(Category category);
    Page<Product> findByCategory(Category category, Pageable pageable);
    List<Product> findByCategoryId(Long categoryId);
    
    // === Recherche par marque ===
    List<Product> findByBrandIgnoreCase(String brand);
    Page<Product> findByBrandIgnoreCase(String brand, Pageable pageable);
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findAllDistinctBrands();
    
    // === Recherche par taille ===
    List<Product> findBySize(String size);
    Page<Product> findBySize(String size, Pageable pageable);
    
    @Query("SELECT DISTINCT p.size FROM Product p WHERE p.size IS NOT NULL ORDER BY p.size")
    List<String> findAllDistinctSizes();
    
    // === Recherche par saison ===
    List<Product> findBySeason(TireSeason season);

    @Query("SELECT DISTINCT p.season FROM Product p WHERE p.season IS NOT NULL ORDER BY p.season")
    List<String> findAllDistinctSeasons();
    
    // === Recherche par prix ===
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    List<Product> findByPriceLessThanEqual(BigDecimal maxPrice);
    List<Product> findByPriceGreaterThanEqual(BigDecimal minPrice);
    
    // === Gestion des stocks ===
    List<Product> findByStockGreaterThan(int minStock);
    List<Product> findByStockLessThanEqual(int maxStock);
    
    @Query("SELECT p FROM Product p WHERE p.stock = 0")
    List<Product> findOutOfStockProducts();
    
    @Query("SELECT p FROM Product p WHERE p.stock <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);
    
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
    
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId")
    void increaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
    
    // === Recherche combinée avancée ===
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
           "(:size IS NULL OR p.size = :size) AND " +
           "(:season IS NULL OR LOWER(p.season) = LOWER(:season)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "p.stock > 0")
    Page<Product> findWithFilters(@Param("category") Category category,
                                 @Param("brand") String brand,
                                 @Param("size") String size, 
                                 @Param("season") String season,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 Pageable pageable);
    
    // === Recherche textuelle ===
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.size) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Product> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // === Statistiques et recommandations ===
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY SIZE(p.orderItems) DESC")
    Page<Product> findPopular(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.createdAt >= :date")
    List<Product> findRecentProducts(@Param("date") LocalDateTime date);
    
    // === Statistiques et recommandations ===
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRating(@Param("productId") Long productId);

    // === Dimensions (width/profile/diameter) sur la base du champ size ===
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock > 0 AND " +
           "(:width IS NULL OR LOWER(p.size) LIKE LOWER(CONCAT('%', :width, '%'))) AND " +
           "(:profile IS NULL OR LOWER(p.size) LIKE LOWER(CONCAT('%', :profile, '%'))) AND " +
           "(:diameter IS NULL OR LOWER(p.size) LIKE LOWER(CONCAT('%', :diameter, '%')))")
    Page<Product> findByDimensions(@Param("width") String width,
                                   @Param("profile") String profile,
                                   @Param("diameter") String diameter,
                                   Pageable pageable);
}