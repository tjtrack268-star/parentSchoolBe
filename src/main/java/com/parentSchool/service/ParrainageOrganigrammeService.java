package com.parentSchool.service;

import com.parentSchool.dto.EleveNode;
import com.parentSchool.dto.TuteurOrganigrammeNode;
import com.parentSchool.entity.Eleve;
import com.parentSchool.entity.Tuteur;
import com.parentSchool.repository.EleveRepository;
import com.parentSchool.repository.TuteurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParrainageOrganigrammeService {

    private final TuteurRepository tuteurRepository;
    private final EleveRepository eleveRepository;

    public List<TuteurOrganigrammeNode> getOrganigrammeParrainage() {
        return tuteurRepository.findAll().stream()
            .sorted(Comparator.comparing(Tuteur::getNom, String.CASE_INSENSITIVE_ORDER))
            .map(this::buildTuteurNode)
            .toList();
    }

    public List<EleveNode> getElevesSansTuteur() {
        return eleveRepository.findByTuteurIsNullOrderByNomCompletAsc().stream()
            .map(this::toEleveNode)
            .toList();
    }

    private TuteurOrganigrammeNode buildTuteurNode(Tuteur tuteur) {
        List<EleveNode> eleves = eleveRepository.findByTuteurIdOrderByNomCompletAsc(tuteur.getId()).stream()
            .map(this::toEleveNode)
            .toList();

        TuteurOrganigrammeNode node = new TuteurOrganigrammeNode();
        node.setTuteurId(tuteur.getId());
        node.setNomTuteur(tuteur.getNom());
        node.setNombreEleves(eleves.size());
        node.setEleves(eleves);
        return node;
    }

    private EleveNode toEleveNode(Eleve eleve) {
        return new EleveNode(
            eleve.getId(),
            eleve.getNomComplet(),
            eleve.getCodeMembre(),
            eleve.getPays()
        );
    }
}
