package com.parentSchool.service;

import com.parentSchool.entity.Payment;
import com.parentSchool.entity.User;
import com.parentSchool.enums.PaymentType;
import com.parentSchool.enums.UserType;
import com.parentSchool.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    public BigDecimal getMembershipFee(UserType userType) {
        return switch (userType) {
            case ORDINARY -> new BigDecimal("5000");
            case HONOR -> new BigDecimal("20000");
            case BENEFACTOR -> new BigDecimal("5000"); // Même tarif que ordinaire
        };
    }
    
    public Payment createMembershipPayment(User user) {
        Payment payment = new Payment();
        payment.setPayer(user);
        payment.setAmount(getMembershipFee(user.getUserType()));
        payment.setPaymentType(PaymentType.MEMBERSHIP);
        payment.setCurrency("XAF");
        return paymentRepository.save(payment);
    }
}