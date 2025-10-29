package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Favori;
import com.pneumaliback.www.entity.Product;
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
public interface FavoriRepository extends JpaRepository<Favori, Long> {

    List<Favori> findByUser(User user);

    List<Favori> findByUserOrderByCreatedAtDesc(User user);

    Page<Favori> findByUser(User user, Pageable pageable);

    List<Favori> findByProduct(Product product);

    Optional<Favori> findByUserAndProduct(User user, Product product);

    @Query("SELECT f FROM Favori f WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    Page<Favori> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM Favori f WHERE f.product.id = :productId")
    List<Favori> findByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(f) FROM Favori f WHERE f.user = :user")
    Long countByUser(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM Favori f WHERE f.product = :product")
    Long countByProduct(@Param("product") Product product);

    @Query("SELECT f FROM Favori f WHERE f.tags IS NOT NULL AND LOWER(f.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    List<Favori> findByTagsContainingIgnoreCase(@Param("tag") String tag);

    @Query("SELECT f FROM Favori f WHERE f.user = :user AND f.tags IS NOT NULL AND LOWER(f.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    List<Favori> findByUserAndTagsContainingIgnoreCase(@Param("user") User user, @Param("tag") String tag);

    @Query("SELECT f FROM Favori f WHERE f.personalComment IS NOT NULL AND f.personalComment != ''")
    List<Favori> findAllWithComments();

    @Query("SELECT f FROM Favori f WHERE f.user = :user AND f.personalComment IS NOT NULL AND f.personalComment != ''")
    List<Favori> findByUserWithComments(@Param("user") User user);

    @Query("SELECT f FROM Favori f WHERE f.user = :user AND LOWER(f.personalComment) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Favori> findByUserAndCommentContainingIgnoreCase(@Param("user") User user, @Param("keyword") String keyword);

    boolean existsByUserAndProduct(User user, Product product);

    void deleteByUserAndProduct(User user, Product product);

    void deleteByUser(User user);

    void deleteByProduct(Product product);
}