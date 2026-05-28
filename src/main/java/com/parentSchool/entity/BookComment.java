package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_comments")
@Data
public class BookComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bookSlug;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private String authorName;

    private Integer rating = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
