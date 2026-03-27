package com.mustafa.service.impl;

import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.entity.Company;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.service.IInternalCompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalCompanyServiceImpl implements IInternalCompanyService {

    private final ICompanyRepository companyRepository;

    @Override
    public void syncNewCompany(CompanySyncRequest request) {
        log.info("SERVICE: Yeni şirket Kurumsal Veritabanına işleniyor. Adı: {}, Kimlik: {}",
                request.getCompanyName(), request.getCompanyIdentityNumber());

        // 🚀 DÜZELTİLDİ: Senin gönderdiğin Entity'deki builder metodlarıyla birebir eşleşti!
        Company newCompany = Company.builder()
                .companyIdentityNumber(request.getCompanyIdentityNumber())
                .companyName(request.getCompanyName())
                .contactEmail(request.getContactEmail())
                .taxOffice(request.getTaxOffice())
                // .autoSalaryPaymentEnabled(false) -> Entity'nde @Builder.Default olduğu için yazmaya bile gerek yok, kendi false atanacak!
                .build();

        companyRepository.save(newCompany);
        log.info("✅ SERVICE: Şirket başarıyla kaydedildi!");
    }
}