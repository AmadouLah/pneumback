package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    // === Actifs ===
    List<Brand> findByActiveTrue();

    List<Brand> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    Optional<Brand> findByName(String name);

    Optional<Brand> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Brand> findByNameContainingIgnoreCase(String name);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.brand.id = :brandId AND p.brand.active = true")
    long countProductsInBrand(@Param("brandId") Long brandId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.brand.id = :brandId")
    long countAllProductsInBrand(@Param("brandId") Long brandId);
}
