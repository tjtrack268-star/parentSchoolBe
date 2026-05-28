package com.parentSchool.service;

import com.parentSchool.controller.SponsorImportRequest;
import com.parentSchool.entity.SponsorDirectoryEntry;
import com.parentSchool.entity.User;
import com.parentSchool.enums.UserStatus;
import com.parentSchool.enums.UserType;
import com.parentSchool.repository.SponsorDirectoryRepository;
import com.parentSchool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SponsorDirectoryService {
    private final SponsorDirectoryRepository sponsorDirectoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public int importNetwork(SponsorImportRequest request) {
        List<SponsorDirectoryEntry> entries = new ArrayList<>();
        for (SponsorImportRequest.SponsorNode node : request.getReseau_parrainage()) {
            flatten(node, entries);
        }
        sponsorDirectoryRepository.deleteAll();
        sponsorDirectoryRepository.saveAll(entries);
        return entries.size();
    }

    private void flatten(SponsorImportRequest.SponsorNode node, List<SponsorDirectoryEntry> entries) {
        SponsorDirectoryEntry entry = new SponsorDirectoryEntry();
        entry.setFullName(node.getId() != null ? node.getId().trim() : "");
        entry.setSponsorCode(node.getCode() != null && !node.getCode().isBlank() ? node.getCode().trim().toUpperCase() : null);
        entry.setProfession(node.getProfession());
        entry.setCountry(node.getPays());
        entry.setRootNode(Boolean.TRUE.equals(node.getRoot()));
        entries.add(entry);

        if (node.getFilleuls() != null) {
            for (SponsorImportRequest.SponsorNode child : node.getFilleuls()) {
                flatten(child, entries);
            }
        }
    }

    public List<SponsorDirectoryEntry> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        return sponsorDirectoryRepository.search(query.trim());
    }

    @Transactional
    public Map<String, Object> migrateNetworkToUsers(SponsorImportRequest request) {
        return migrateNetworkToUsers(request, "Imported@123");
    }

    @Transactional
    public Map<String, Object> migrateNetworkToUsers(SponsorImportRequest request, String defaultPassword) {
        List<FlatNode> flatNodes = new ArrayList<>();
        for (SponsorImportRequest.SponsorNode node : request.getReseau_parrainage()) {
            flattenWithParent(node, null, null, flatNodes);
        }

        int created = 0;
        int updated = 0;
        Map<String, User> usersByNodeKey = new HashMap<>();
        Map<String, User> usersByCode = new HashMap<>();
        Map<String, User> usersByName = new HashMap<>();

        for (FlatNode n : flatNodes) {
            User user = resolveUser(n.code, n.name);
            boolean isNew = false;
            if (user == null) {
                user = new User();
                isNew = true;
            }

            String[] names = splitName(n.name);
            user.setFirstName(names[0]);
            user.setLastName(names[1]);
            user.setUserType(user.getUserType() != null ? user.getUserType() : UserType.ORDINARY);
            user.setUserStatus(user.getUserStatus() != null ? user.getUserStatus() : UserStatus.ACTIVE);
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(defaultPassword);
            }
            if (n.code != null && !n.code.isBlank()) {
                user.setSponsorshipCode(n.code.toUpperCase(Locale.ROOT));
            }
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                user.setEmail(generateUniqueEmail(n.name, n.code));
            }
            if (user.getCity() == null || user.getCity().isBlank()) {
                user.setCity((n.country != null && !n.country.isBlank()) ? n.country : "N/A");
            }

            User saved = userRepository.save(user);
            usersByNodeKey.put(n.nodeKey, saved);
            if (n.code != null && !n.code.isBlank()) usersByCode.put(n.code.toUpperCase(Locale.ROOT), saved);
            usersByName.put(normalizeName(n.name), saved);

            if (isNew) created++;
            else updated++;
        }

        int linked = 0;
        for (FlatNode n : flatNodes) {
            if (n.parentKey == null) continue;
            User child = usersByNodeKey.get(n.nodeKey);
            User sponsor = usersByNodeKey.get(n.parentKey);
            if (child == null || sponsor == null) continue;
            if (child.getId().equals(sponsor.getId())) continue;

            if (child.getSponsor() == null || !child.getSponsor().getId().equals(sponsor.getId())) {
                child.setSponsor(sponsor);
                userRepository.save(child);
                linked++;
            }
        }

        // Recompute direct sponsorship counters and points (60 pts per direct referral)
        for (User u : userRepository.findAll()) {
            int directCount = userRepository.findBySponsor(u).size();
            u.setDirectSponsorshipsCount(directCount);
            u.setTotalPoints(directCount * 60);
            userRepository.save(u);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("nodes", flatNodes.size());
        report.put("createdUsers", created);
        report.put("updatedUsers", updated);
        report.put("linkedSponsors", linked);
        return report;
    }

    private void flattenWithParent(
        SponsorImportRequest.SponsorNode node,
        String parentKey,
        String parentName,
        List<FlatNode> acc
    ) {
        String name = node.getId() != null ? node.getId().trim() : "";
        String code = node.getCode() != null ? node.getCode().trim() : "";
        String nodeKey = (code.isBlank() ? normalizeName(name) : code.toUpperCase(Locale.ROOT)) + "#" + acc.size();
        acc.add(new FlatNode(nodeKey, parentKey, name, code, node.getPays()));

        if (node.getFilleuls() != null) {
            for (SponsorImportRequest.SponsorNode child : node.getFilleuls()) {
                flattenWithParent(child, nodeKey, name, acc);
            }
        }
    }

    private User resolveUser(String code, String name) {
        if (code != null && !code.isBlank()) {
            return userRepository.findBySponsorshipCodeIgnoreCase(code.trim()).orElse(null);
        }
        List<User> byName = userRepository.findByFullNameExact(name);
        if (byName.size() == 1) return byName.get(0);
        return null;
    }

    private String[] splitName(String fullName) {
        String cleaned = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) return new String[]{"Inconnu", "Inconnu"};
        String[] parts = cleaned.split(" ", 2);
        if (parts.length == 1) return new String[]{parts[0], "Membre"};
        return new String[]{parts[0], parts[1]};
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String generateUniqueEmail(String name, String code) {
        String base = (code != null && !code.isBlank())
            ? code.toLowerCase(Locale.ROOT)
            : normalizeName(name).replaceAll("[^a-z0-9]+", ".");
        if (base.isBlank()) base = "member";
        base = base.replaceAll("^\\.+|\\.+$", "");
        if (base.isBlank()) base = "member";

        String email = base + "@import.parentschool.local";
        int index = 1;
        while (userRepository.findByEmail(email).isPresent()) {
            email = base + "." + index + "@import.parentschool.local";
            index++;
        }
        return email;
    }

    private record FlatNode(String nodeKey, String parentKey, String name, String code, String country) {}
}
