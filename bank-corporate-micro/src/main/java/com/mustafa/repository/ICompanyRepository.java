package com.mustafa.repository;

import com.mustafa.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ICompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByContactEmail(String contactEmail);
    boolean existsByContactEmail(String contactEmail);

    // 🚀 Yeni: Doğrudan şirketin kimlik numarası ile arama
    Optional<Company> findByCompanyIdentityNumber(String identityNumber);

    List<Company> findByAutoSalaryPaymentEnabledTrueAndSalaryPaymentDay(Integer day);
}