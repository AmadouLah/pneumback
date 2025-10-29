package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.Delivery;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    
    Optional<Delivery> findByOrder(Order order);
    
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    
    List<Delivery> findByStatus(DeliveryStatus status);
    
    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);
    
    List<Delivery> findByAddress(Address address);
    
    List<Delivery> findByStatusOrderByCreatedAtAsc(DeliveryStatus status);
    
    @Query("SELECT d FROM Delivery d WHERE d.order.user.id = :userId")
    List<Delivery> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT d FROM Delivery d WHERE d.order.user.id = :userId ORDER BY d.createdAt DESC")
    Page<Delivery> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT d FROM Delivery d WHERE d.address.city = :city AND d.status = :status")
    List<Delivery> findByAddressCityAndStatus(@Param("city") String city, @Param("status") DeliveryStatus status);
    
    @Query("SELECT d FROM Delivery d WHERE d.address.country = :country AND d.status = :status")
    List<Delivery> findByAddressCountryAndStatus(@Param("country") String country, @Param("status") DeliveryStatus status);
    
    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    Long countByStatus(@Param("status") DeliveryStatus status);
    
    @Query("SELECT d FROM Delivery d WHERE d.trackingNumber IS NOT NULL ORDER BY d.createdAt DESC")
    List<Delivery> findAllWithTrackingNumber();
    
    boolean existsByTrackingNumber(String trackingNumber);
    
    boolean existsByOrder(Order order);
    
    void deleteByOrder(Order order);
}