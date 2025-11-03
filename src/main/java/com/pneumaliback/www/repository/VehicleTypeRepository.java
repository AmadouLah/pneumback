package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Category;
import com.pneumaliback.www.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findByCategory(Category category);

    List<VehicleType> findByCategoryId(Long categoryId);

    @Query("SELECT v FROM VehicleType v WHERE v.active = true ORDER BY v.name")
    List<VehicleType> findByActiveTrueOrderByNameAsc();

    @Query("SELECT v FROM VehicleType v WHERE v.category = :category AND v.active = true ORDER BY v.name")
    List<VehicleType> findByCategoryAndActiveTrueOrderByNameAsc(@Param("category") Category category);

    @Query("SELECT v FROM VehicleType v WHERE v.category.id = :categoryId AND v.active = true ORDER BY v.name")
    List<VehicleType> findByCategoryIdAndActiveTrueOrderByNameAsc(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.vehicleType.id = :vehicleTypeId")
    long countProductsByVehicleTypeId(@Param("vehicleTypeId") Long vehicleTypeId);

    Optional<VehicleType> findByNameIgnoreCaseAndCategory(String name, Category category);
}
