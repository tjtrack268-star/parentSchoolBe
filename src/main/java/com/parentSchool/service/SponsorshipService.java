package com.parentSchool.service;

import com.parentSchool.entity.Sponsorship;
import com.parentSchool.entity.User;
import com.parentSchool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SponsorshipService {
    
    private final UserRepository userRepository;
    private final GradeService gradeService;
    
    @Transactional
    public void createSponsorship(User sponsor, User sponsored) {
        // Ajouter points et incrémenter compteur parrainage
        sponsor.setTotalPoints(sponsor.getTotalPoints() + 60);
        sponsor.setDirectSponsorshipsCount(sponsor.getDirectSponsorshipsCount() + 1);
        userRepository.save(sponsor);
        
        // Vérifier et mettre à jour le grade automatiquement
        gradeService.checkAndUpdateGrade(sponsor);
    }
}