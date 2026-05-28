package com.parentSchool.repository;

import com.parentSchool.entity.Payment;
import com.parentSchool.entity.User;
import com.parentSchool.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPayer(User payer);
    List<Payment> findByPaymentType(PaymentType paymentType);
}