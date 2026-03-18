package com.mustafa.repository;

import com.mustafa.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ICompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByContactEmail(String contactEmail);
    boolean existsByContactEmail(String contactEmail);
    // Merkezi kimlik numarası (Vergi No) üzerinden şirket profilini bulmak için:
    Optional<Company> findByAppUser_IdentityNumber(String identityNumber);

    // Otomatik ödemesi açık olan ve ödeme günü parametre olarak verilen güne (Örn: ayın 5'ine) eşit olan şirketleri bulur
    List<Company> findByAutoSalaryPaymentEnabledTrueAndSalaryPaymentDay(Integer day);
}