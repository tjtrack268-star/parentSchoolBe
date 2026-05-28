package com.parentSchool.controller;

import com.parentSchool.dto.OrganigrammeNode;
import com.parentSchool.dto.EleveNode;
import com.parentSchool.dto.ExcelImportReport;
import com.parentSchool.dto.TuteurOrganigrammeNode;
import com.parentSchool.entity.User;
import com.parentSchool.entity.Grade;
import com.parentSchool.entity.Payment;
import com.parentSchool.enums.UserRole;
import com.parentSchool.service.UserService;
import com.parentSchool.service.PaymentService;
import com.parentSchool.service.JwtService;
import com.parentSchool.service.OrganigrammeService;
import com.parentSchool.service.ParrainageOrganigrammeService;
import com.parentSchool.service.EleveImportService;
import com.parentSchool.service.SponsorDirectoryService;
import com.parentSchool.repository.GradeRepository;
import com.parentSchool.repository.CommissionRepository;
import com.parentSchool.repository.PaymentRepository;
import com.parentSchool.enums.PaymentType;
import com.parentSchool.entity.Commission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    
    private final UserService userService;
    private final PaymentService paymentService;
    private final GradeRepository gradeRepository;
    private final JwtService jwtService;
    private final OrganigrammeService organigrammeService;
    private final ParrainageOrganigrammeService parrainageOrganigrammeService;
    private final EleveImportService eleveImportService;
    private final CommissionRepository commissionRepository;
    private final PaymentRepository paymentRepository;
    private final SponsorDirectoryService sponsorDirectoryService;
    
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user, @RequestParam(required = false) Long sponsorId, @RequestParam(required = false) String sponsorCode) {
        return userService.registerUser(user, sponsorId, sponsorCode, null);
    }

    @GetMapping("/sponsors/search")
    public ResponseEntity<?> searchSponsors(@RequestParam String q) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();

            for (User u : userService.getAllUsers()) {
                String fullName = (u.getFirstName() + " " + u.getLastName()).trim();
                if (fullName.toLowerCase().contains(q.toLowerCase())
                    || (u.getSponsorshipCode() != null && u.getSponsorshipCode().toLowerCase().contains(q.toLowerCase()))) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("fullName", fullName);
                    item.put("code", u.getSponsorshipCode());
                    item.put("country", u.getCountry() != null ? u.getCountry().getName() : null);
                    item.put("source", "user");
                    results.add(item);
                }
            }

            sponsorDirectoryService.search(q).forEach(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("fullName", entry.getFullName());
                item.put("code", entry.getSponsorCode());
                item.put("country", entry.getCountry());
                item.put("source", "directory");
                results.add(item);
            });

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/sponsors/import-network")
    public ResponseEntity<?> importSponsorNetwork(@RequestBody SponsorImportRequest request) {
        try {
            int count = sponsorDirectoryService.importNetwork(request);
            return ResponseEntity.ok(Map.of("imported", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur import réseau: " + e.getMessage());
        }
    }

    @PostMapping("/sponsors/migrate-network-users")
    public ResponseEntity<?> migrateNetworkToUsers(@RequestBody SponsorImportRequest request) {
        try {
            return ResponseEntity.ok(sponsorDirectoryService.migrateNetworkToUsers(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur migration réseau->users: " + e.getMessage());
        }
    }
    
    @GetMapping("/grades")
    public List<Grade> getAllGrades() {
        return gradeRepository.findAll();
    }
    
    @PostMapping("/payments")
    public Payment createPayment(@RequestBody User user) {
        return paymentService.createMembershipPayment(user);
    }
    
    @GetMapping("/users/{id}/team")
    public List<User> getUserTeam(@PathVariable Long id) {
        return userService.findById(id)
            .map(userService::getTeamMembers)
            .orElse(List.of());
    }

    @GetMapping("/network/me")
    public ResponseEntity<?> getMyNetwork(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.extractEmail(token);
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            OrganigrammeNode networkTree = organigrammeService.getOrganigrammeForUser(user);
            return ResponseEntity.ok(networkTree);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token invalide");
        }
    }
    
    @GetMapping("/organigramme")
    public ResponseEntity<?> getOrganigramme(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(403).body("Accès réservé aux administrateurs");
        }
        return ResponseEntity.ok(organigrammeService.getOrganigramme());
    }
    
    @GetMapping("/organigramme/public")
    public ResponseEntity<?> getPublicOrganigramme() {
        return ResponseEntity.ok(organigrammeService.getPublicOrganigramme());
    }

    @GetMapping("/organigramme/parrainage")
    public ResponseEntity<?> getParrainageOrganigramme(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(403).body("Accès réservé aux administrateurs");
        }
        return ResponseEntity.ok(parrainageOrganigrammeService.getOrganigrammeParrainage());
    }

    @GetMapping("/organigramme/parrainage/sans-tuteur")
    public ResponseEntity<?> getElevesSansTuteur(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(403).body("Accès réservé aux administrateurs");
        }
        return ResponseEntity.ok(parrainageOrganigrammeService.getElevesSansTuteur());
    }

    @PostMapping("/eleves/import")
    public ResponseEntity<?> importEleves(@RequestParam("file") MultipartFile file) {
        try {
            ExcelImportReport report = eleveImportService.importerFichierEleves(file);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur import Excel: " + e.getMessage());
        }
    }
    
    // Missing endpoints
    @GetMapping("/vouchers/my-vouchers")
    public ResponseEntity<?> getMyVouchers(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.extractEmail(token);
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<User> sponsoredUsers = user.getSponsoredUsers() != null ? user.getSponsoredUsers() : List.of();
            int usedVoucherCount = (int) paymentRepository.findByPayer(user).stream()
                .filter(p -> p.getPaymentType() == PaymentType.VOUCHER)
                .count();

            List<Map<String, Object>> vouchers = new ArrayList<>();
            int index = 0;
            for (User sponsored : sponsoredUsers) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "bs-" + user.getId() + "-" + sponsored.getId());
                item.put("code", "BS-" + user.getSponsorshipCode() + "-" + String.format("%03d", index + 1));
                item.put("amount", 3000);
                item.put("isUsed", index < usedVoucherCount);
                item.put("createdAt", sponsored.getCreatedAt());
                vouchers.add(item);
                index++;
            }

            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token invalide");
        }
    }
    
    @GetMapping("/commissions/history")
    public ResponseEntity<?> getCommissionsHistory(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.extractEmail(token);
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<Commission> commissions = commissionRepository.findByBeneficiaryOrderByCalculatedAtDesc(user);
            List<Map<String, Object>> data = new ArrayList<>();

            for (Commission commission : commissions) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", commission.getId());
                item.put("amount", commission.getAmount());
                item.put("createdAt", commission.getCalculatedAt());
                item.put("type", commission.getCommissionType() != null ? commission.getCommissionType().name() : "COMMISSION");
                item.put("description", "Commission generation " + (commission.getGenerationLevel() != null ? commission.getGenerationLevel() : 1));
                data.add(item);
            }

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token invalide");
        }
    }
    
    @GetMapping("/referrals-data")
    public List<Map<String, Object>> getReferralsData() {
        // Placeholder implementation - returns empty list for now
        return new ArrayList<>();
    }
    
    @GetMapping("/fetch-user")
    public ResponseEntity<?> fetchUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.extractEmail(token);
            
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId().toString());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("totalPoints", user.getTotalPoints());
            response.put("directReferrals", user.getSponsoredUsers() != null ? user.getSponsoredUsers().size() : 0);
            response.put("currentGrade", user.getCurrentGrade() != null ? user.getCurrentGrade().getName() : "Aucun");
            response.put("userRole", user.getUserRole() != null ? user.getUserRole().name() : null);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token invalide");
        }
    }
    
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Vérifier email unique
            if (userService.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
                return ResponseEntity.badRequest().body("Cet email est déjà utilisé.");
            }

            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail().trim().toLowerCase());
            user.setPassword(request.getPassword());
            user.setPhone(request.getPhone());
            user.setUserType(request.getUserType() != null ? request.getUserType() : com.parentSchool.enums.UserType.ORDINARY);
            user.setCity(request.getCity());

            User savedUser = userService.registerUser(user, request.getSponsorId(), request.getSponsorCode(), request.getSponsorName());

            // Retourner token + user comme le login
            String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getId());
            return ResponseEntity.ok(new LoginResponse(token, savedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }
    
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body("Email requis.");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body("Mot de passe requis.");
            }

            String email = request.getEmail().trim().toLowerCase();

            User user = userService.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("Aucun compte trouvé avec cet email.");
            }

            if (!user.getPassword().equals(request.getPassword())) {
                return ResponseEntity.badRequest().body("Mot de passe incorrect.");
            }

            String token = jwtService.generateToken(user.getEmail(), user.getId());
            return ResponseEntity.ok(new LoginResponse(token, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    private boolean isAdmin(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.extractEmail(token);
            User user = userService.findByEmail(email).orElse(null);
            return user != null && user.getUserRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
}
