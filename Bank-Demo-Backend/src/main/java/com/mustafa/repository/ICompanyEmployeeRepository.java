package com.mustafa.repository;

import com.mustafa.entity.CompanyEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ICompanyEmployeeRepository extends JpaRepository<CompanyEmployee, Long> {
    // Bir şirketin tüm çalışanlarını getirmek için:
    List<CompanyEmployee> findByCompanyId(Long companyId);

    // Bir bireyin hangi şirkette çalıştığını bulmak için:
    Optional<CompanyEmployee> findByRetailCustomerId(Long retailCustomerId);

    // Personel bu şirkette zaten çalışıyor mu kontrolü için
    boolean existsByCompany_IdAndRetailCustomer_AppUser_IdentityNumber(Long companyId, String identityNumber);

    // Silme veya Güncelleme işlemi için spesifik personeli bulmak için
    Optional<CompanyEmployee> findByCompany_IdAndRetailCustomer_AppUser_IdentityNumber(Long companyId, String identityNumber);
}