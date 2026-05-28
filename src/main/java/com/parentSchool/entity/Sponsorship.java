package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsorships")
@Data
public class Sponsorship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "sponsor_id")
    private User sponsor;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "sponsored_id")
    private User sponsored;
    
    @Column(name = "sponsorship_code", unique = true)
    private String sponsorshipCode;
    
    @Column(name = "points_earned")
    private Integer pointsEarned = 60;
    
    @Enumerated(EnumType.STRING)
    private SponsorshipStatus status = SponsorshipStatus.VALIDATED;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    @ManyToOne
    @JoinColumn(name = "validated_by_id")
    private User validatedBy;
    
    public enum SponsorshipStatus {
        PENDING, VALIDATED, REJECTED
    }
}