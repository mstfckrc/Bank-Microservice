package com.mustafa.service.impl;

import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.service.IInternalBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalBankServiceImpl implements IInternalBankService {

    private final IAccountRepository accountRepository;

    @Override
    public AccountValidationResponse validateAccount(String iban) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new BankOperationException("Kasa bulunamadı!"));

        return AccountValidationResponse.builder()
                .ownerIdentityNumber(account.getOwnerIdentityNumber()) // Sadece TC dönüyor!
                .isActive(account.isActive())
                .build();
    }

    // 🚀 YENİ EKLENDİ: İçinde bakiye olan kasaları koruyan iptal/silme mekanizması
    @Override
    @Transactional
    public void deleteCustomerAccounts(String identityNumber) {
        // 1. Kullanıcının tüm hesaplarını bul
        // (Not: Repository'nde findByOwnerIdentityNumber veya findByIdentityNumber hangisi varsa o eşleşmeli. Entity'den yola çıkarak Owner kullandım)
        List<Account> accounts = accountRepository.findByOwnerIdentityNumber(identityNumber);

        // 2. İçinde bakiye olan hesap var mı kontrol et (Kritik İş Kuralı!)
        boolean hasBalance = accounts.stream()
                .anyMatch(acc -> acc.getBalance().compareTo(BigDecimal.ZERO) > 0);

        if (hasBalance) {
            log.error("HATA: {} numaralı müşterinin içinde bakiye olan hesabı var, silinemez!", identityNumber);
            throw new BankOperationException("Müşterinin bakiyesi olan hesapları var. Önce bakiyeler sıfırlanmalıdır!");
        }

        // 🚀 3. Bakiye yoksa hesapları pasife çek (Soft Delete)
        // Geçmiş dekontları olduğu için SQL Hard Delete yapmamıza izin vermez!
        accounts.forEach(acc -> acc.setActive(false));
        accountRepository.saveAll(accounts);
        log.info("SERVICE: {} numaralı müşterinin tüm kasaları başarıyla PASİFE alındı.", identityNumber);
    }
}
