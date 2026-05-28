package com.parentSchool.service;

import com.parentSchool.dto.ExcelImportReport;
import com.parentSchool.entity.User;
import com.parentSchool.enums.UserRole;
import com.parentSchool.enums.UserStatus;
import com.parentSchool.enums.UserType;
import com.parentSchool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Colonnes du fichier xlsx (index 0-based) :
 * 0  = N°
 * 1  = Nom
 * 2  = Code membre
 * 3  = Profession
 * 4  = Pays
 * 5  = Parrain / Tuteur (nom)
 * 6  = Code Parrain
 * 7  = Niveau
 * 8  = Nb Filleuls
 * 9  = USERNAME
 * 10 = MOT DE PASSE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EleveImportService {

    private final UserRepository userRepository;
    private final GradeService gradeService;

    private static final String DEFAULT_PASSWORD = "user123";

    private record MembreRow(
        Integer num,
        String nom,
        String code,
        String profession,
        String pays,
        String nomParrain,
        String codeParrain,
        String email  // colonne 9 contient déjà l'email complet
    ) {}

    @Transactional
    public ExcelImportReport importerFichierEleves(MultipartFile file) {
        validateFile(file);
        ExcelImportReport report = new ExcelImportReport();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(Locale.FRENCH);

            // ── Passe 1 : lire toutes les lignes (skip ligne 0 = header) ──────
            List<MembreRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isEmptyRow(row, fmt)) continue;

                String nom = cell(row, 1, fmt);
                if (nom == null || nom.isBlank()) continue;

                report.setLignesLues(report.getLignesLues() + 1);

                rows.add(new MembreRow(
                    parseInteger(cell(row, 0, fmt)),
                    nom.trim(),
                    cell(row, 2, fmt),   // Code membre
                    cell(row, 3, fmt),   // Profession
                    cell(row, 4, fmt),   // Pays
                    cell(row, 5, fmt),   // Nom parrain
                    cell(row, 6, fmt),   // Code parrain
                    cell(row, 9, fmt)    // Email complet (ex: colette.emadouan@parentschool.com)
                ));
            }

            log.info("Lignes lues depuis xlsx : {}", rows.size());

            // ── Passe 2 : construire les index depuis la DB ───────────────────
            Map<String, User> byCode = new HashMap<>();
            Map<String, User> byName = new HashMap<>();

            userRepository.findAll().forEach(u -> {
                if (u.getSponsorshipCode() != null)
                    byCode.put(u.getSponsorshipCode().toUpperCase(), u);
                byName.put(norm(u.getFirstName() + " " + u.getLastName()), u);
            });

            // ── Passe 3 : créer / mettre à jour les users ────────────────────
            for (MembreRow r : rows) {
                String code = sanitize(r.code());

                User user = code != null ? byCode.get(code) : null;
                if (user == null) user = byName.get(norm(r.nom()));

                boolean isNew = (user == null);
                if (isNew) user = new User();

                // Nom
                String[] names = splitName(r.nom());
                user.setFirstName(names[0]);
                user.setLastName(names[1]);

                // Code
                if (code != null) user.setSponsorshipCode(code);

                // Email : utiliser directement la valeur du xlsx (colonne 9)
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    String emailFromXlsx = r.email();
                    if (emailFromXlsx != null && emailFromXlsx.contains("@")) {
                        // Email complet fourni dans le fichier
                        user.setEmail(uniqueEmail(emailFromXlsx.trim().toLowerCase()));
                    } else if (emailFromXlsx != null && !emailFromXlsx.isBlank()) {
                        // Juste un username sans domaine
                        user.setEmail(uniqueEmail(emailFromXlsx.trim().toLowerCase() + "@parentschool.com"));
                    } else {
                        // Fallback sur le code
                        String base = code != null ? code.toLowerCase() : "member" + r.num();
                        user.setEmail(uniqueEmail(base + "@parentschool.com"));
                    }
                }

                // Mot de passe
                if (user.getPassword() == null || user.getPassword().isBlank())
                    user.setPassword(DEFAULT_PASSWORD);

                // Phone : null si pas de numéro (plus de contrainte unique)
                if (user.getPhone() == null || user.getPhone().isBlank()) {
                    if (r.num() != null && r.num() > 0)
                        user.setPhone(String.format("+000%07d", r.num()));
                }

                // Ville / pays
                if (r.pays() != null && !r.pays().isBlank())
                    user.setCity(r.pays().trim());

                user.setUserType(UserType.ORDINARY);
                user.setUserRole(UserRole.MEMBER);
                user.setUserStatus(UserStatus.ACTIVE);

                User saved = userRepository.save(user);

                // Mettre à jour les index
                if (code != null) byCode.put(code, saved);
                byName.put(norm(r.nom()), saved);

                if (isNew) report.setElevesCrees(report.getElevesCrees() + 1);
                else       report.setElevesMisAJour(report.getElevesMisAJour() + 1);
            }

            // ── Passe 4 : résoudre les parrains ──────────────────────────────
            User admin = userRepository.findAdmin().orElse(null);
            int liens = 0;

            for (MembreRow r : rows) {
                String code = sanitize(r.code());
                User child = code != null ? byCode.get(code) : byName.get(norm(r.nom()));
                if (child == null) continue;

                // Chercher parrain par code parrain d'abord, puis par nom
                User sponsor = null;
                String codeParrain = sanitize(r.codeParrain());
                if (codeParrain != null) sponsor = byCode.get(codeParrain);
                if (sponsor == null && r.nomParrain() != null && !r.nomParrain().isBlank())
                    sponsor = byName.get(norm(r.nomParrain()));
                if (sponsor == null) sponsor = admin;

                // Ne pas s'auto-parrainer
                if (sponsor != null && !sponsor.getId().equals(child.getId())) {
                    if (child.getSponsor() == null
                            || !child.getSponsor().getId().equals(sponsor.getId())) {
                        child.setSponsor(sponsor);
                        userRepository.save(child);
                        liens++;
                    }
                }
            }

            report.setTuteursCrees(liens);
            log.info("Liens parrain créés/mis à jour : {}", liens);

            // ── Passe 5 : recalculer stats et grades ──────────────────────────
            userRepository.findAll().forEach(u -> {
                int count = userRepository.findBySponsor(u).size();
                u.setDirectSponsorshipsCount(count);
                u.setTotalPoints(count * 60);
                userRepository.save(u);
            });

            userRepository.findAll().forEach(u -> {
                try { gradeService.checkAndUpdateGrade(u); }
                catch (Exception e) { log.warn("Grade skip {}: {}", u.getEmail(), e.getMessage()); }
            });

            log.info("Import terminé : {} créés, {} mis à jour, {} liens",
                report.getElevesCrees(), report.getElevesMisAJour(), liens);

            return report;

        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier Excel : " + e.getMessage(), e);
        }
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    /** Garde le code original, tronqué à 16 chars max */
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim().toUpperCase();
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    /** Normalise un nom pour comparaison insensible à la casse et aux espaces */
    private String norm(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Décompose "DUPONT Jean Pierre" → ["Jean Pierre", "DUPONT"]
     * Heuristique : premier mot tout en majuscules = nom de famille
     */
    private String[] splitName(String fullName) {
        String cleaned = fullName.trim().replaceAll("\\s+", " ");
        String[] parts = cleaned.split(" ", 2);
        if (parts.length == 1) return new String[]{parts[0], parts[0]};
        if (parts[0].equals(parts[0].toUpperCase())
                && parts[0].matches("[A-ZÀÂÄÉÈÊËÎÏÔÙÛÜÇ]+")) {
            return new String[]{parts[1], parts[0]};
        }
        return new String[]{parts[0], parts[1]};
    }

    private String uniqueEmail(String email) {
        if (!userRepository.findByEmail(email).isPresent()) return email;
        // Si doublon, ajouter un suffixe avant le @
        String[] parts = email.split("@", 2);
        int idx = 1;
        String candidate;
        do {
            candidate = parts[0] + "." + idx++ + "@" + parts[1];
        } while (userRepository.findByEmail(candidate).isPresent());
        return candidate;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.replaceAll("\\.0$", "").trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Fichier vide ou absent.");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new IllegalArgumentException("Format non supporté. Utilise un fichier .xlsx");
    }

    private boolean isEmptyRow(Row row, DataFormatter fmt) {
        if (row == null) return true;
        for (int i = 0; i <= 9; i++) {
            String v = cell(row, i, fmt);
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }

    private String cell(Row row, int idx, DataFormatter fmt) {
        if (row == null) return null;
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        String v = fmt.formatCellValue(c);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
