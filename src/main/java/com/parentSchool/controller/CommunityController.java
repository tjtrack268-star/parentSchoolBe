package com.parentSchool.controller;

import com.parentSchool.entity.*;
import com.parentSchool.repository.*;
import com.parentSchool.service.JwtService;
import com.parentSchool.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommunityController {

    private final TestimonialRepository testimonialRepository;
    private final BookCommentRepository bookCommentRepository;
    private final BookLikeRepository bookLikeRepository;
    private final JwtService jwtService;
    private final UserService userService;

    // ── TÉMOIGNAGES ──────────────────────────────────────────────────────────

    @GetMapping("/testimonials")
    public List<Map<String, Object>> getTestimonials() {
        return testimonialRepository.findByApprovedTrueOrderByCreatedAtDesc()
            .stream().map(this::toTestimonialMap).toList();
    }

    @PostMapping("/testimonials")
    public ResponseEntity<?> createTestimonial(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Testimonial t = new Testimonial();
            t.setContent((String) body.get("content"));
            t.setAuthorName((String) body.get("authorName"));
            t.setAuthorCity((String) body.getOrDefault("authorCity", ""));
            t.setAuthorRole((String) body.getOrDefault("authorRole", ""));
            t.setRating(body.get("rating") != null ? (Integer) body.get("rating") : 5);
            t.setApproved(false); // modération

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String email = jwtService.extractEmail(authHeader.substring(7));
                userService.findByEmail(email).ifPresent(t::setUser);
            }

            Testimonial saved = testimonialRepository.save(t);
            return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "message", "Témoignage soumis avec succès. Il sera publié après modération."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    // Admin : approuver un témoignage
    @PutMapping("/testimonials/{id}/approve")
    public ResponseEntity<?> approveTestimonial(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(403).body("Accès refusé");
        return testimonialRepository.findById(id).map(t -> {
            t.setApproved(true);
            testimonialRepository.save(t);
            return ResponseEntity.ok(Map.of("message", "Témoignage approuvé"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Admin : liste tous les témoignages (y compris non approuvés)
    @GetMapping("/testimonials/all")
    public ResponseEntity<?> getAllTestimonials(@RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) return ResponseEntity.status(403).body("Accès refusé");
        return ResponseEntity.ok(testimonialRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toTestimonialMap).toList());
    }

    // ── COMMENTAIRES OUVRAGES ─────────────────────────────────────────────────

    @GetMapping("/books/{slug}/comments")
    public List<Map<String, Object>> getComments(@PathVariable String slug) {
        return bookCommentRepository.findByBookSlugOrderByCreatedAtDesc(slug)
            .stream().map(this::toCommentMap).toList();
    }

    @PostMapping("/books/{slug}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String slug,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            BookComment c = new BookComment();
            c.setBookSlug(slug);
            c.setContent((String) body.get("content"));
            c.setAuthorName((String) body.get("authorName"));
            c.setRating(body.get("rating") != null ? (Integer) body.get("rating") : 5);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String email = jwtService.extractEmail(authHeader.substring(7));
                userService.findByEmail(email).ifPresent(u -> {
                    c.setUser(u);
                    c.setAuthorName(u.getFirstName() + " " + u.getLastName());
                });
            }

            BookComment saved = bookCommentRepository.save(c);
            return ResponseEntity.ok(toCommentMap(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    // ── LIKES OUVRAGES ────────────────────────────────────────────────────────

    @GetMapping("/books/{slug}/likes")
    public Map<String, Object> getLikes(
            @PathVariable String slug,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        long count = bookLikeRepository.countByBookSlug(slug);
        boolean liked = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String email = jwtService.extractEmail(authHeader.substring(7));
                User user = userService.findByEmail(email).orElse(null);
                if (user != null) liked = bookLikeRepository.existsByBookSlugAndUser(slug, user);
            } catch (Exception ignored) {}
        }

        return Map.of("count", count, "liked", liked);
    }

    @PostMapping("/books/{slug}/likes")
    public ResponseEntity<?> toggleLike(
            @PathVariable String slug,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String email = jwtService.extractEmail(authHeader.substring(7));
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Optional<BookLike> existing = bookLikeRepository.findByBookSlugAndUser(slug, user);
            if (existing.isPresent()) {
                bookLikeRepository.delete(existing.get());
            } else {
                BookLike like = new BookLike();
                like.setBookSlug(slug);
                like.setUser(user);
                bookLikeRepository.save(like);
            }

            long count = bookLikeRepository.countByBookSlug(slug);
            boolean liked = existing.isEmpty();
            return ResponseEntity.ok(Map.of("count", count, "liked", liked));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> toTestimonialMap(Testimonial t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("content", t.getContent());
        m.put("authorName", t.getAuthorName());
        m.put("authorCity", t.getAuthorCity());
        m.put("authorRole", t.getAuthorRole());
        m.put("rating", t.getRating());
        m.put("approved", t.getApproved());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    private Map<String, Object> toCommentMap(BookComment c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("content", c.getContent());
        m.put("authorName", c.getAuthorName());
        m.put("rating", c.getRating());
        m.put("createdAt", c.getCreatedAt());
        return m;
    }

    private boolean isAdmin(String authHeader) {
        try {
            String email = jwtService.extractEmail(authHeader.substring(7));
            return userService.findByEmail(email)
                .map(u -> u.getUserRole().name().equals("ADMIN"))
                .orElse(false);
        } catch (Exception e) { return false; }
    }
}
