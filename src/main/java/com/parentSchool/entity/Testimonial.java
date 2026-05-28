package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "testimonials")
@Data
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private String authorName;

    private String authorCity;
    private String authorRole;

    @Column(nullable = false)
    private Integer rating = 5;

    @Column(nullable = false)
    private Boolean approved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
