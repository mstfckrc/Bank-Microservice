package com.mustafa.service.impl;

import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.entity.Company;
import com.mustafa.entity.CompanyEmployee;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.ICompanyEmployeeRepository;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.service.IInternalCompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalCompanyServiceImpl implements IInternalCompanyService {

    private final ICompanyRepository companyRepository;
    private final ICompanyEmployeeRepository companyEmployeeRepository;

    @Override
    @Transactional
    public void syncNewCompany(CompanySyncRequest request) {
        // Mevcut Repository metoduyla kontrol
        if (companyRepository.findByCompanyIdentityNumber(request.getCompanyIdentityNumber()).isPresent()) {
            log.warn("Kurumsal firma zaten mevcut: {}", request.getCompanyIdentityNumber());
            return;
        }

        Company company = Company.builder()
                .companyIdentityNumber(request.getCompanyIdentityNumber())
                .companyName(request.getCompanyName())
                .taxOffice(request.getTaxOffice())
                .contactEmail(request.getContactEmail())
                .autoSalaryPaymentEnabled(false)
                .build();

        companyRepository.save(company);
        log.info("SERVICE: Kurumsal firma ({}) sisteme senkronize edildi.", request.getCompanyIdentityNumber());
    }

    @Override
    @Transactional
    public void updateCompanyInfo(String identityNumber, CompanySyncRequest request) {
        Company company = companyRepository.findByCompanyIdentityNumber(identityNumber)
                .orElseThrow(() -> new BankOperationException("Güncellenecek kurumsal firma bulunamadı!"));

        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            company.setCompanyName(request.getCompanyName());
        }
        if (request.getContactEmail() != null && !request.getContactEmail().isBlank()) {
            company.setContactEmail(request.getContactEmail());
        }

        companyRepository.save(company);
        log.info("SERVICE: Kurumsal firma ({}) bilgileri başarıyla güncellendi.", identityNumber);
    }

    @Override
    @Transactional
    public void deleteCompanyInfo(String identityNumber) {
        Company company = companyRepository.findByCompanyIdentityNumber(identityNumber)
                .orElseThrow(() -> new BankOperationException("Silinecek kurumsal firma bulunamadı!"));

        // 1. Personel listesini al
        List<CompanyEmployee> employees = companyEmployeeRepository.findByCompanyId(company.getId());

        // 2. Personelleri toplu sil
        companyEmployeeRepository.deleteAll(employees);

        // 3. Şirketi sil
        companyRepository.delete(company);
        log.info("SERVICE: Kurumsal firma ({}) ve bağlı tüm çalışanları başarıyla silindi.", identityNumber);
    }
}