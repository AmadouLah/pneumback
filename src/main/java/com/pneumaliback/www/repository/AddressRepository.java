package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByUser(User user);
    
    List<Address> findByUserOrderByCreatedAtDesc(User user);
    
    List<Address> findByCountry(Country country);
    
    Page<Address> findByUser(User user, Pageable pageable);
    
    List<Address> findByCity(String city);
    
    List<Address> findByCityAndCountry(String city, Country country);
    
    List<Address> findByRegion(String region);
    
    List<Address> findByPostalCode(String postalCode);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId")
    List<Address> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<Address> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT a FROM Address a WHERE a.user = :user AND a.country = :country")
    List<Address> findByUserAndCountry(@Param("user") User user, @Param("country") Country country);
    
    boolean existsByUserAndStreetAndCityAndCountry(User user, String street, String city, Country country);
    
    Optional<Address> findByIdAndUser(Long id, User user);
    
    void deleteByUser(User user);
}