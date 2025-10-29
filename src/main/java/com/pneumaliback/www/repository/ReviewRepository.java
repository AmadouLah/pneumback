package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.Review;
import com.pneumaliback.www.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByProduct(Product product);
    
    List<Review> findByProductOrderByCreatedAtDesc(Product product);
    
    Page<Review> findByProduct(Product product, Pageable pageable);
    
    List<Review> findByUser(User user);
    
    Page<Review> findByUser(User user, Pageable pageable);
    
    List<Review> findByRating(int rating);
    
    List<Review> findByRatingGreaterThanEqual(int rating);
    
    List<Review> findByRatingLessThanEqual(int rating);
    
    List<Review> findByRatingBetween(int minRating, int maxRating);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findByProductId(@Param("productId") Long productId);
    
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<Review> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId ORDER BY r.rating DESC")
    List<Review> findByProductIdOrderByRatingDesc(@Param("productId") Long productId);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.rating >= :minRating")
    List<Review> findByProductIdAndRatingGreaterThanEqual(@Param("productId") Long productId, @Param("minRating") int minRating);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product")
    Double findAverageRatingByProduct(@Param("product") Product product);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product")
    Long countByProduct(@Param("product") Product product);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product AND r.rating = :rating")
    Long countByProductAndRating(@Param("product") Product product, @Param("rating") int rating);
    
    @Query("SELECT r FROM Review r WHERE r.product = :product AND r.comment IS NOT NULL AND r.comment != ''")
    List<Review> findByProductWithComments(@Param("product") Product product);
    
    @Query("SELECT r FROM Review r WHERE r.comment IS NOT NULL AND r.comment != '' ORDER BY r.createdAt DESC")
    Page<Review> findAllWithComments(Pageable pageable);
    
    Optional<Review> findByUserAndProduct(User user, Product product);
    
    boolean existsByUserAndProduct(User user, Product product);
    
    @Query("SELECT r FROM Review r WHERE LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Review> findByCommentContainingIgnoreCase(@Param("keyword") String keyword);
    
    void deleteByUser(User user);
    
    void deleteByProduct(Product product);
}