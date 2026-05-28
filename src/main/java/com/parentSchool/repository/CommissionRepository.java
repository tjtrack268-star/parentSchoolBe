package com.parentSchool.repository;

import com.parentSchool.entity.Commission;
import com.parentSchool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {
    List<Commission> findByBeneficiaryOrderByCalculatedAtDesc(User beneficiary);
}

