package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "countries")
@Data
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, length = 3)
    private String code;
    
    @Column(length = 10)
    private String currency = "FCFA";
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToOne
    @JoinColumn(name = "focal_point_id")
    private User focalPoint;
}