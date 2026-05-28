package com.parentSchool.entity;

import com.parentSchool.enums.PaymentMethod;
import com.parentSchool.enums.PaymentStatus;
import com.parentSchool.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "payer_id")
    private User payer;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 10)
    private String currency = "FCFA";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}