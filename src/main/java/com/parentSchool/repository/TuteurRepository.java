package com.parentSchool.repository;

import com.parentSchool.entity.Tuteur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TuteurRepository extends JpaRepository<Tuteur, Long> {
    Optional<Tuteur> findByNomIgnoreCase(String nom);
}
