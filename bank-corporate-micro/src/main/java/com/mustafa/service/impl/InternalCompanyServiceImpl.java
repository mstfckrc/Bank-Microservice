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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalCompanyServiceImpl implements IInternalCompanyService {

    private final ICompanyRepository companyRepository;
    private final ICompanyEmployeeRepository companyEmployeeRepository;

    @Override
    @Transactional
    public void syncNewCompany(CompanySyncRequest request) {
        var existing = companyRepository.findByCompanyIdentityNumber(request.getCompanyIdentityNumber());
        if (existing.isPresent()) {
            Company company = existing.get();
            if (!Objects.equals(company.getCompanyName(), request.getCompanyName())
                    || !Objects.equals(company.getContactEmail(), request.getContactEmail())
                    || !Objects.equals(company.getTaxOffice(), request.getTaxOffice())) {
                throw new BankOperationException("Mevcut şirket kaydı gönderilen bilgilerle uyuşmuyor; yönetici incelemesi gerekiyor.");
            }
            log.info("Aynı şirket kayıt isteği tekrarlandı; mevcut kayıt korundu.");
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
        log.info("SERVICE: Yeni şirket kaydı senkronize edildi.");
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
        log.info("SERVICE: Şirket bilgileri güncellendi.");
    }

    @Override
    @Transactional
    public void deleteCompanyInfo(String identityNumber) {
        var existing = companyRepository.findByCompanyIdentityNumber(identityNumber);
        if (existing.isEmpty()) {
            log.info("Şirket silme isteği tekrarlandı; kayıt zaten bulunmuyor.");
            return;
        }
        Company company = existing.get();

        // 1. Personel listesini al
        List<CompanyEmployee> employees = companyEmployeeRepository.findByCompanyId(company.getId());

        // 2. Personelleri toplu sil
        companyEmployeeRepository.deleteAll(employees);

        // 3. Şirketi sil
        companyRepository.delete(company);
        log.info("SERVICE: Şirket ve bağlı çalışan kayıtları silindi.");
    }

    // 🚀 YENİ EKLENDİ: Karargahın bilgi okuyabilmesi için
    @Override
    @Transactional(readOnly = true)
    public CompanySyncRequest getCompanyInfo(String identityNumber) {
        Company company = companyRepository.findByCompanyIdentityNumber(identityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal firma bulunamadı!"));

        return CompanySyncRequest.builder()
                .companyIdentityNumber(company.getCompanyIdentityNumber())
                .companyName(company.getCompanyName())
                .taxOffice(company.getTaxOffice())
                .contactEmail(company.getContactEmail())
                .build();
    }
}
