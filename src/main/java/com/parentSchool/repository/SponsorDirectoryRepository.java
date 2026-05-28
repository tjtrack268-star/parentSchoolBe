package com.parentSchool.repository;

import com.parentSchool.entity.SponsorDirectoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorDirectoryRepository extends JpaRepository<SponsorDirectoryEntry, Long> {
    Optional<SponsorDirectoryEntry> findBySponsorCodeIgnoreCase(String sponsorCode);

    @Query("SELECT s FROM SponsorDirectoryEntry s WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(COALESCE(s.sponsorCode, '')) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.fullName ASC")
    List<SponsorDirectoryEntry> search(String query);
}

