package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Country;
import com.pneumaliback.www.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
       // === Recherche de base ===
       Optional<User> findByEmailIgnoreCase(String email);

       boolean existsByEmailIgnoreCase(String email);

       // === Recherche par rôle ===
       List<User> findByRole(Role role);

       Page<User> findByRole(Role role, Pageable pageable);

       long countByRole(Role role);

       // === Recherche par statut ===
       List<User> findByEnabledTrue();

       List<User> findByEnabledFalse();

       Page<User> findByEnabled(boolean enabled, Pageable pageable);

       // === Comptes verrouillés ===
       @Query("SELECT u FROM User u WHERE u.accountNonLocked = false AND u.lockTime < :currentTime")
       List<User> findExpiredLockedAccounts(@Param("currentTime") Instant currentTime);

       @Modifying
       @Query("UPDATE User u SET u.accountNonLocked = true, u.failedAttempts = 0, u.lockTime = null WHERE u.id = :userId")
       void unlockUser(@Param("userId") Long userId);

       @Modifying
       @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.id = :userId")
       void incrementFailedAttempts(@Param("userId") Long userId);

       // === Recherche avancée ===
       @Query("SELECT u FROM User u WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))")
       List<User> findByFullNameContainingIgnoreCase(@Param("name") String name);

       @Query("SELECT u FROM User u WHERE u.phoneNumber LIKE :phonePattern")
       List<User> findByPhoneNumberLike(@Param("phonePattern") String phonePattern);

       // === Recherche par pays ===
       List<User> findByCountry(Country country);

       Page<User> findByCountry(Country country, Pageable pageable);

       long countByCountry(Country country);

       List<User> findByCountryAndRole(Country country, Role role);

       Page<User> findByCountryAndRole(Country country, Role role, Pageable pageable);

       List<User> findByCountryAndEnabled(Country country, boolean enabled);

       // === Statistiques ===
       @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
       long countNewUsersFrom(@Param("startDate") Instant startDate);

       @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
       List<Object[]> countUsersByRole();

       // === Statistiques par pays ===
       @Query("SELECT u.country, COUNT(u) FROM User u GROUP BY u.country ORDER BY COUNT(u) DESC")
       List<Object[]> countUsersByCountry();

       @Query("SELECT u.country, u.role, COUNT(u) FROM User u GROUP BY u.country, u.role ORDER BY u.country, u.role")
       List<Object[]> countUsersByCountryAndRole();

       @Query("SELECT COUNT(u) FROM User u WHERE u.country = :country AND u.createdAt >= :startDate")
       long countNewUsersByCountryFrom(@Param("country") Country country, @Param("startDate") Instant startDate);

       // === Utilisateurs par région géographique ===
       @Query("SELECT u FROM User u WHERE u.country IN :countries")
       List<User> findByCountryIn(@Param("countries") List<Country> countries);

       @Query("SELECT u FROM User u WHERE u.country IN :countries AND u.enabled = true")
       List<User> findActiveUsersByCountries(@Param("countries") List<Country> countries);

       // === Top pays par activité ===
       @Query("SELECT u.country, COUNT(u) FROM User u WHERE u.enabled = true GROUP BY u.country ORDER BY COUNT(u) DESC")
       List<Object[]> findTopCountriesByActiveUsers();

       Optional<User> findByEmail(String email);

       boolean existsByEmail(String email);

       Optional<User> findByVerificationCode(String verificationCode);

       @Query("SELECT u FROM User u WHERE u.email = :email AND u.accountNonLocked = true")
       Optional<User> findByEmailAndAccountNonLocked(@Param("email") String email);

       @Query("SELECT COUNT(u) FROM User u WHERE u.email = :email AND u.failedAttempts >= :maxAttempts")
       long countByEmailAndFailedAttemptsGreaterThanEqual(@Param("email") String email,
                     @Param("maxAttempts") int maxAttempts);
       // ===== MÉTHODES SUPPLÉMENTAIRES UTILES =====

       // === Validation et recherche avancée ===
       @Query("SELECT u FROM User u WHERE u.phoneNumber LIKE :countryPrefix AND u.country = :country")
       List<User> findByCountryAndMatchingPhonePrefix(@Param("country") Country country,
                     @Param("countryPrefix") String countryPrefix);

       // === Clients potentiels par expansion géographique ===
       @Query("SELECT u FROM User u WHERE u.country = :country AND u.role = 'CLIENT' AND u.enabled = true")
       List<User> findActiveClientsByCountry(@Param("country") Country country);

       @Query("SELECT COUNT(u) FROM User u WHERE u.country = :country AND u.role = 'CLIENT' AND u.enabled = true")
       long countActiveClientsByCountry(@Param("country") Country country);

       // === Influenceurs par pays ===
       @Query("SELECT u FROM User u WHERE u.country = :country AND u.role = 'INFLUENCEUR' AND u.enabled = true")
       List<User> findActiveInfluencersByCountry(@Param("country") Country country);

       // === Recherche pour expansion business ===
       @Query("SELECT u.country, COUNT(u) FROM User u WHERE u.role = 'CLIENT' AND u.createdAt >= :startDate " +
                     "GROUP BY u.country ORDER BY COUNT(u) DESC")
       List<Object[]> findGrowthOpportunitiesByCountry(@Param("startDate") Instant startDate);

       // === Méthodes de validation métier ===
       default boolean isValidPhoneForCountry(String phoneNumber, Country country) {
              if (phoneNumber == null || country == null)
                     return false;
              return phoneNumber.startsWith(country.getPhonePrefix()) ||
                            phoneNumber.startsWith(country.getPhonePrefix().substring(1)); // Sans le +
       }

       // === Recherche multi-critères pour le marketing ===
       @Query("SELECT u FROM User u WHERE u.country = :country AND u.role = :role " +
                     "AND u.enabled = true AND u.createdAt BETWEEN :startDate AND :endDate")
       List<User> findTargetUsersForMarketing(@Param("country") Country country,
                     @Param("role") Role role,
                     @Param("startDate") Instant startDate,
                     @Param("endDate") Instant endDate);
}
