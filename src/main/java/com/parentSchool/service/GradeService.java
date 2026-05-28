package com.parentSchool.service;

import com.parentSchool.entity.Grade;
import com.parentSchool.entity.User;
import com.parentSchool.repository.GradeRepository;
import com.parentSchool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GradeService {
    
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public void checkAndUpdateGrade(User user) {
        Integer sponsorships = user.getDirectSponsorshipsCount();
        Integer points = user.getTotalPoints();
        
        Optional<Grade> newGrade = gradeRepository.findHighestEligibleGrade(sponsorships, points);
        
        if (newGrade.isPresent()) {
            Grade grade = newGrade.get();
            // Toujours mettre à jour le grade selon les nouveaux critères
            user.setCurrentGrade(grade);
            userRepository.save(user);
        } else {
            // Aucun grade éligible, retirer le grade actuel
            user.setCurrentGrade(null);
            userRepository.save(user);
        }
    }
}