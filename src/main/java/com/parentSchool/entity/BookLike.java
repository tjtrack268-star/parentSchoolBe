package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"book_slug", "user_id"})
})
@Data
public class BookLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_slug", nullable = false)
    private String bookSlug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
