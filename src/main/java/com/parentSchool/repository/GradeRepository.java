package com.parentSchool.repository;

import com.parentSchool.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByName(String name);
    
    @Query("SELECT g FROM Grade g WHERE g.requiredSponsorships <= :sponsorships AND g.requiredPoints <= :points ORDER BY g.level DESC LIMIT 1")
    Optional<Grade> findHighestEligibleGrade(Integer sponsorships, Integer points);
}