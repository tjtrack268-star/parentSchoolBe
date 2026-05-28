package com.parentSchool.controller;

import com.parentSchool.entity.User;
import com.parentSchool.enums.UserType;
import com.parentSchool.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final UserService userService;
    
    @PostMapping("/create-user")
    public ResponseEntity<?> createTestUser() {
        try {
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@email.com");
            user.setPassword("password123");
            user.setUserType(UserType.ORDINARY);
            user.setCity("Yaoundé");
            
            User savedUser = userService.registerUser(user, null, null, null);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }
}
