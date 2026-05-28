package com.parentSchool.service;

import com.parentSchool.dto.OrganigrammeNode;
import com.parentSchool.entity.User;
import com.parentSchool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganigrammeService {
    
    private final UserRepository userRepository;
    
    public List<OrganigrammeNode> getOrganigramme() {
        // Récupérer l'admin (racine de l'organigramme)
        User admin = userRepository.findAdmin().orElse(null);
        if (admin == null) return List.of();
        
        return List.of(buildOrganigrammeNode(admin));
    }
    
    public List<OrganigrammeNode> getPublicOrganigramme() {
        // Récupérer tous les utilisateurs sans sponsor (racines)
        List<User> rootUsers = userRepository.findBySponsorIsNull();
        
        return rootUsers.stream()
            .map(this::buildPublicOrganigrammeNode)
            .collect(Collectors.toList());
    }

    public OrganigrammeNode getOrganigrammeForUser(User user) {
        if (user == null) {
            return null;
        }
        return buildOrganigrammeNode(user);
    }
    
    private OrganigrammeNode buildOrganigrammeNode(User user) {
        String gradeName = user.getCurrentGrade() != null ? user.getCurrentGrade().getName() : "Aucun";
        
        OrganigrammeNode node = new OrganigrammeNode(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getSponsorshipCode(),
            gradeName,
            user.getDirectSponsorshipsCount(),
            user.getTotalPoints()
        );
        
        // Récupérer les filleuls (enfants dans l'organigramme)
        List<User> children = userRepository.findBySponsor(user);
        List<OrganigrammeNode> childNodes = children.stream()
            .map(this::buildOrganigrammeNode)
            .collect(Collectors.toList());
        
        node.setChildren(childNodes);
        return node;
    }
    
    private OrganigrammeNode buildPublicOrganigrammeNode(User user) {
        String gradeName = user.getCurrentGrade() != null ? user.getCurrentGrade().getName() : "Aucun";
        
        // Version publique sans email pour la confidentialité
        OrganigrammeNode node = new OrganigrammeNode(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            null, // Pas d'email dans la version publique
            user.getSponsorshipCode(),
            gradeName,
            user.getDirectSponsorshipsCount(),
            user.getTotalPoints()
        );
        
        // Récupérer les filleuls (enfants dans l'organigramme)
        List<User> children = userRepository.findBySponsor(user);
        List<OrganigrammeNode> childNodes = children.stream()
            .map(this::buildPublicOrganigrammeNode)
            .collect(Collectors.toList());
        
        node.setChildren(childNodes);
        return node;
    }
}
