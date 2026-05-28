package com.parentSchool.service;

import com.parentSchool.entity.*;
import com.parentSchool.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final SponsorshipService sponsorshipService;
    private final PaymentService paymentService;
    private final GradeService gradeService;
    private final EmailService emailService;
    
    private static final String DEFAULT_SPONSOR_CODE = "ADMIN001";
    
    @Transactional
    public User registerUser(User user, Long sponsorId, String sponsorCode, String sponsorName) {
        // Utiliser le code par défaut si aucun code fourni
        String finalSponsorCode = (sponsorCode == null || sponsorCode.isBlank()) ? DEFAULT_SPONSOR_CODE : sponsorCode.trim();
        
        // Définir le parrain par ID ou par code
        if (sponsorId != null) {
            Optional<User> sponsor = userRepository.findById(sponsorId);
            sponsor.ifPresent(user::setSponsor);
        } else if (!finalSponsorCode.isBlank()) {
            Optional<User> sponsor = userRepository.findBySponsorshipCodeIgnoreCase(finalSponsorCode);
            sponsor.ifPresent(user::setSponsor);
        } else if (sponsorName != null && !sponsorName.isBlank()) {
            List<User> candidates = userRepository.findByFullNameExact(sponsorName.trim());
            if (candidates.size() == 1) {
                user.setSponsor(candidates.get(0));
            } else if (candidates.size() > 1) {
                throw new IllegalArgumentException("Plusieurs parrains trouvés avec ce nom. Utilisez le code de parrainage.");
            } else {
                throw new IllegalArgumentException("Parrain introuvable avec ce nom.");
            }
        }

        if (user.getSponsor() == null) {
            Optional<User> fallbackSponsor = userRepository.findBySponsorshipCodeIgnoreCase(DEFAULT_SPONSOR_CODE);
            fallbackSponsor.ifPresent(user::setSponsor);
        }
        
        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);
        
        // Créer le parrainage si sponsor existe
        if (savedUser.getSponsor() != null) {
            sponsorshipService.createSponsorship(savedUser.getSponsor(), savedUser);
        }
        
        // Créer le paiement d'adhésion (async)
        try {
            paymentService.createMembershipPayment(savedUser);
        } catch (Exception e) {
            // Log error but don't fail registration
        }
        
        // Envoyer l'email de bienvenue (async)
        try {
            String resolvedSponsorName = savedUser.getSponsor() != null ? 
                savedUser.getSponsor().getFirstName() + " " + savedUser.getSponsor().getLastName() : null;
            
            emailService.sendWelcomeEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getSponsorshipCode(),
                resolvedSponsorName
            );
        } catch (Exception e) {
            // Log error but don't fail registration
        }
        
        return savedUser;
    }
    
    @Transactional
    public void updateUserGrade(User user) {
        gradeService.checkAndUpdateGrade(user);
    }
    
    public List<User> getTeamMembers(User user) {
        return userRepository.findBySponsor(user);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findBySponsorshipCode(String code) {
        return userRepository.findBySponsorshipCode(code);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAllOrderByName();
    }
    
    public User getDefaultSponsor() {
        return userRepository.findAdmin().orElse(null);
    }
}
