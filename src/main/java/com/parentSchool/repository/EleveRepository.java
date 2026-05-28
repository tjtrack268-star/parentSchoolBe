package com.parentSchool.repository;

import com.parentSchool.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EleveRepository extends JpaRepository<Eleve, Long> {
    List<Eleve> findByTuteurIdOrderByNomCompletAsc(Long tuteurId);
    List<Eleve> findByTuteurIsNullOrderByNomCompletAsc();
    Optional<Eleve> findByCodeMembreIgnoreCase(String codeMembre);
}
