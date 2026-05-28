package com.parentSchool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    public void sendWelcomeEmail(String userEmail, String firstName, String sponsorshipCode, String sponsorName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("Bienvenue dans ParentSchool !");
        message.setText(String.format(
            "Bonjour %s,\n\n" +
            "Bienvenue dans ParentSchool !\n\n" +
            "Votre code de parrainage : %s\n" +
            "Votre parrain : %s\n\n" +
            "Utilisez votre code de parrainage pour inviter d'autres membres.\n\n" +
            "Cordialement,\n" +
            "L'équipe ParentSchool",
            firstName, sponsorshipCode, sponsorName != null ? sponsorName : "Aucun"
        ));
        
        mailSender.send(message);
    }
}