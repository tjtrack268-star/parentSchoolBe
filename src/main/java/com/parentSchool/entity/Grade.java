package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "grades")
@Data
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(name = "grade_level")
    private Integer level;
    
    @Column(name = "required_sponsorships")
    private Integer requiredSponsorships;
    
    @Column(name = "required_points")
    private Integer requiredPoints;
    
    @Column(name = "benefit_amount", precision = 10, scale = 2)
    private BigDecimal benefitAmount;
    
    @Column(name = "direct_commission_rate", precision = 5, scale = 2)
    private BigDecimal directCommissionRate = BigDecimal.ZERO;
    
    @Column(name = "team_commission_rate", precision = 5, scale = 2)
    private BigDecimal teamCommissionRate = BigDecimal.ZERO;
}