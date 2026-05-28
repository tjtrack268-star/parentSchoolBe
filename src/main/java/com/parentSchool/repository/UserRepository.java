package com.parentSchool.repository;

import com.parentSchool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findBySponsorshipCode(String sponsorshipCode);
    Optional<User> findBySponsorshipCodeIgnoreCase(String sponsorshipCode);
    List<User> findBySponsor(User sponsor);
    List<User> findBySponsorIsNull();
    
    @Query("SELECT u FROM User u WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) = LOWER(:fullName)")
    List<User> findByFullNameExact(String fullName);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.sponsor = :sponsor")
    Long countDirectSponsorships(User sponsor);
    
    @Query("SELECT u FROM User u WHERE u.country.id = :countryId")
    List<User> findByCountryId(Long countryId);
    
    @Query("SELECT u FROM User u WHERE u.userRole = 'ADMIN' ORDER BY u.id ASC LIMIT 1")
    Optional<User> findAdmin();
    
    @Query("SELECT u FROM User u ORDER BY u.firstName, u.lastName")
    List<User> findAllOrderByName();
}
