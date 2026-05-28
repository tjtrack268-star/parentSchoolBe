package com.parentSchool.entity;

import com.parentSchool.enums.UserStatus;
import com.parentSchool.enums.UserType;
import com.parentSchool.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType = UserType.ORDINARY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus userStatus = UserStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole userRole = UserRole.MEMBER;
    
    @Column(name = "total_points")
    private Integer totalPoints = 0;
    
    @Column(name = "direct_sponsorships_count")
    private Integer directSponsorshipsCount = 0;
    
    @Column(name = "sponsorship_code", unique = true, length = 16)
    private String sponsorshipCode;
    
    private String city;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    private User sponsor;
    
    @ManyToOne
    @JoinColumn(name = "current_grade_id")
    private Grade currentGrade;
    
    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;
    
    @OneToMany(mappedBy = "sponsor", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> sponsoredUsers;
    
    @PrePersist
    public void prePersist() {
        if (sponsorshipCode == null) {
            sponsorshipCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}