package com.parentSchool.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parentSchool.entity.Grade;
import com.parentSchool.entity.User;
import com.parentSchool.enums.UserRole;
import com.parentSchool.enums.UserStatus;
import com.parentSchool.enums.UserType;
import com.parentSchool.repository.GradeRepository;
import com.parentSchool.repository.UserRepository;
import com.parentSchool.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final GradeService gradeService;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_PASSWORD = "user123";

    @Override
    public void run(String... args) throws Exception {
        initializeGrades();
        initializeAdmin();
        importNetworkJson();
        updateAllUserGrades();
    }

    // ─── Grades ──────────────────────────────────────────────────────────────

    private void initializeGrades() {
        if (gradeRepository.count() > 0) return;
        gradeRepository.save(grade("Leader",        1,  4,   240,  5_000,  0,    0));
        gradeRepository.save(grade("Leader Senior", 2,  8,  1200, 10_000,  0,    5));
        gradeRepository.save(grade("Coordinateur",  3, 18,  3000, 15_000, 10,    5));
        gradeRepository.save(grade("Mentor",        4, 30, 10000, 25_000, 10,    5));
        gradeRepository.save(grade("Directeur",     5, 50, 30000, 50_000, 15,  7.5));
        log.info("Grades initialisés");
    }

    private Grade grade(String name, int level, int sponsorships, int points,
                        double benefit, double directRate, double teamRate) {
        Grade g = new Grade();
        g.setName(name);
        g.setLevel(level);
        g.setRequiredSponsorships(sponsorships);
        g.setRequiredPoints(points);
        g.setBenefitAmount(BigDecimal.valueOf(benefit));
        g.setDirectCommissionRate(BigDecimal.valueOf(directRate));
        g.setTeamCommissionRate(BigDecimal.valueOf(teamRate));
        return g;
    }

    // ─── Admin ───────────────────────────────────────────────────────────────

    private void initializeAdmin() {
        if (userRepository.findBySponsorshipCode("ADMIN001").isPresent()) return;

        User admin = new User();
        admin.setFirstName("CLEMS");
        admin.setLastName("Fondateur");
        admin.setEmail("admin@parentschool.com");
        admin.setPassword(DEFAULT_PASSWORD);
        admin.setPhone("+237000000000");
        admin.setUserType(UserType.HONOR);
        admin.setUserRole(UserRole.ADMIN);
        admin.setUserStatus(UserStatus.ACTIVE);
        admin.setCity("Yaoundé");
        admin.setSponsorshipCode("ADMIN001");
        userRepository.save(admin);
        log.info("Admin CLEMS créé");
    }

    // ─── Import JSON ─────────────────────────────────────────────────────────

    private void importNetworkJson() {
        // Skip si déjà importé (plus de 1 utilisateur)
        if (userRepository.count() > 1) {
            log.info("Réseau déjà importé ({} utilisateurs), skip.", userRepository.count());
            return;
        }

        try {
            String json = readJsonFile();
            if (json == null) {
                log.warn("reseau_parrainage.json introuvable, import ignoré.");
                return;
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode reseau = root.get("reseau_parrainage");
            if (reseau == null || !reseau.isArray()) return;

            // Map code -> User pour résoudre les relations
            Map<String, User> codeToUser = new HashMap<>();

            // Récupérer l'admin comme parrain racine
            User admin = userRepository.findBySponsorshipCode("ADMIN001").orElseThrow();
            codeToUser.put("ADMIN001", admin);

            // Parcours récursif : d'abord les enfants directs du fondateur
            for (JsonNode memberNode : reseau) {
                importNode(memberNode, admin, codeToUser);
            }

            log.info("Import réseau terminé : {} utilisateurs au total", userRepository.count());

        } catch (Exception e) {
            log.error("Erreur import réseau JSON : {}", e.getMessage(), e);
        }
    }

    private void importNode(JsonNode node, User sponsor, Map<String, User> codeToUser) {
        String fullId   = node.path("id").asText("").trim();
        String code     = node.path("code").asText("").trim();
        int    num      = node.path("num").asInt(0);
        String pays     = node.path("pays").asText("").trim();
        String metier   = node.path("profession").asText("").trim();

        if (fullId.isEmpty()) return;

        // Générer un code unique si absent
        String sponsorshipCode = code.isEmpty()
                ? "PS" + String.format("%06d", num)
                : sanitizeCode(code);

        // Éviter les doublons
        if (codeToUser.containsKey(sponsorshipCode)
                || userRepository.findBySponsorshipCode(sponsorshipCode).isPresent()) {
            // Continuer quand même pour les enfants
            User existing = codeToUser.getOrDefault(sponsorshipCode,
                    userRepository.findBySponsorshipCode(sponsorshipCode).orElse(null));
            if (existing != null) {
                JsonNode filleuls = node.get("filleuls");
                if (filleuls != null && filleuls.isArray()) {
                    for (JsonNode child : filleuls) {
                        importNode(child, existing, codeToUser);
                    }
                }
            }
            return;
        }

        // Décomposer le nom complet en prénom / nom
        String[] names = splitName(fullId);

        User user = new User();
        user.setFirstName(names[0]);
        user.setLastName(names[1]);
        user.setEmail(buildEmail(sponsorshipCode, num));
        user.setPassword(DEFAULT_PASSWORD);
        user.setPhone(buildPhone(num));
        user.setUserType(UserType.ORDINARY);
        user.setUserRole(UserRole.MEMBER);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setSponsorshipCode(sponsorshipCode);
        user.setSponsor(sponsor);
        if (!pays.isEmpty()) user.setCity(pays);

        User saved = userRepository.save(user);
        codeToUser.put(sponsorshipCode, saved);
        log.debug("Importé : {} {} ({})", names[0], names[1], sponsorshipCode);

        // Récursion sur les filleuls
        JsonNode filleuls = node.get("filleuls");
        if (filleuls != null && filleuls.isArray()) {
            for (JsonNode child : filleuls) {
                importNode(child, saved, codeToUser);
            }
        }
    }

    // ─── Mise à jour des grades ───────────────────────────────────────────────

    private void updateAllUserGrades() {
        // Recalculer directSponsorshipsCount pour chaque utilisateur
        userRepository.findAll().forEach(user -> {
            int count = userRepository.findBySponsor(user).size();
            user.setDirectSponsorshipsCount(count);
            user.setTotalPoints(count * 60);
            userRepository.save(user);
        });

        // Appliquer les grades
        userRepository.findAll().forEach(user -> {
            try {
                gradeService.checkAndUpdateGrade(user);
            } catch (Exception e) {
                log.warn("Erreur grade pour {} : {}", user.getEmail(), e.getMessage());
            }
        });

        log.info("Grades mis à jour pour tous les utilisateurs");
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private String readJsonFile() throws Exception {
        Path workspaceFile = Path.of("reseau_parrainage.json");
        if (Files.exists(workspaceFile)) return Files.readString(workspaceFile);

        ClassPathResource resource = new ClassPathResource("reseau_parrainage.json");
        if (resource.exists()) return Files.readString(resource.getFile().toPath());

        return null;
    }

    /**
     * Garde le code original du JSON, tronqué à 16 chars max.
     * Ex: "PS237_001" -> "PS237_001" (conservé tel quel)
     */
    private String sanitizeCode(String code) {
        String clean = code.trim().toUpperCase();
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    /**
     * Décompose "DUPONT Jean Pierre" en ["Jean Pierre", "DUPONT"].
     * Heuristique : premier mot tout en majuscules = nom de famille.
     */
    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        if (parts.length == 1) return new String[]{parts[0], parts[0]};

        // Si le premier mot est tout en majuscules, c'est le nom
        if (parts[0].equals(parts[0].toUpperCase()) && parts[0].matches("[A-ZÀÂÄÉÈÊËÎÏÔÙÛÜÇ]+")) {
            return new String[]{parts[1], parts[0]};
        }
        return new String[]{parts[0], parts[1]};
    }

    private String buildEmail(String code, int num) {
        return "user." + code.toLowerCase() + "@parentschool.com";
    }

    private String buildPhone(int num) {
        return String.format("+000%07d", num);
    }
}
