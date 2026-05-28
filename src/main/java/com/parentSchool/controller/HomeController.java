package com.parentSchool.controller;

import com.parentSchool.entity.Grade;
import com.parentSchool.entity.User;
import com.parentSchool.enums.UserRole;
import com.parentSchool.repository.GradeRepository;
import com.parentSchool.repository.UserRepository;
import com.parentSchool.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    
    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("defaultSponsor", userService.getDefaultSponsor());
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@ModelAttribute User user, 
                          @RequestParam(required = false) Long sponsorId,
                          Model model) {
        try {
            userService.registerUser(user, sponsorId, null, null);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l'inscription");
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("defaultSponsor", userService.getDefaultSponsor());
            return "register";
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // TODO: Récupérer utilisateur connecté depuis session
        // Pour test, on prend le premier utilisateur non-admin
        User user = userRepository.findAll().stream()
            .filter(u -> u.getUserRole() != UserRole.ADMIN)
            .findFirst().orElse(null);
            
        if (user != null) {
            model.addAttribute("user", user);
            
            // Trouver le prochain grade
            Grade nextGrade = gradeRepository.findAll().stream()
                .filter(g -> user.getCurrentGrade() == null || g.getLevel() > user.getCurrentGrade().getLevel())
                .min((g1, g2) -> Integer.compare(g1.getLevel(), g2.getLevel()))
                .orElse(null);
            model.addAttribute("nextGrade", nextGrade);
        }
        
        return "dashboard";
    }
}
