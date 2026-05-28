package com.parentSchool.controller;

import com.parentSchool.entity.Payment;
import com.parentSchool.entity.User;
import com.parentSchool.service.PaymentService;
import com.parentSchool.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final UserService userService;
    
    @GetMapping("/membership/{userId}")
    public String membershipPayment(@PathVariable Long userId, Model model) {
        // TODO: Ajouter vérification authentification
        User user = userService.findById(userId).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("amount", paymentService.getMembershipFee(user.getUserType()));
        return "payment/membership";
    }
    
    @PostMapping("/process")
    public String processPayment(@RequestParam Long userId, 
                               @RequestParam String paymentMethod,
                               Model model) {
        try {
            User user = userService.findById(userId).orElseThrow();
            Payment payment = paymentService.createMembershipPayment(user);
            // TODO: Intégrer API de paiement (Mobile Money, etc.)
            return "redirect:/dashboard?payment=success";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du paiement");
            return "payment/membership";
        }
    }
}