package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commissions")
@Data
public class Commission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "beneficiary_id")
    private User beneficiary;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "source_user_id")
    private User sourceUser;
    
    @ManyToOne
    @JoinColumn(name = "source_payment_id")
    private Payment sourcePayment;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;
    
    @Column(name = "generation_level")
    private Integer generationLevel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type")
    private CommissionType commissionType;
    
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt = LocalDateTime.now();
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    public enum CommissionType {
        DIRECT_SPONSORSHIP, TEAM_SPONSORSHIP
    }
}