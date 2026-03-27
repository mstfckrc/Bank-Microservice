package com.mustafa.repository;

import com.mustafa.entity.CompanyEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ICompanyEmployeeRepository extends JpaRepository<CompanyEmployee, Long> {

    List<CompanyEmployee> findByCompanyId(Long companyId);

    // 🚀 Yeni: Çalışanı direkt TC'si ile bulma
    Optional<CompanyEmployee> findByEmployeeIdentityNumber(String employeeIdentityNumber);

    // 🚀 Yeni: JOIN zinciri kırıldı. Sadece Şirket ID ve Çalışan TC'si yetiyor!
    boolean existsByCompany_IdAndEmployeeIdentityNumber(Long companyId, String employeeIdentityNumber);

    // 🚀 Yeni: Silme/Güncelleme için spesifik personeli bulma metodu
    Optional<CompanyEmployee> findByCompany_IdAndEmployeeIdentityNumber(Long companyId, String employeeIdentityNumber);
}