package com.mustafa.service.impl;

import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
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
    private final ITransactionRepository transactionRepository;

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
        List<Account> accounts = accountRepository.findByOwnerIdentityNumber(identityNumber);

        boolean hasBalance = accounts.stream()
                .anyMatch(acc -> acc.getBalance().compareTo(BigDecimal.ZERO) != 0);

        if (hasBalance) {
            log.warn("Müşteri hesap kapatma talebi mevcut bakiye nedeniyle reddedildi.");
            throw new BankOperationException("Müşterinin bakiyesi olan hesapları var. Önce bakiyeler sıfırlanmalıdır!");
        }

        if (transactionRepository.existsByStatusAndSenderAccount_OwnerIdentityNumber(
                Transaction.TransactionStatus.PENDING_APPROVAL, identityNumber)
                || transactionRepository.existsByStatusAndReceiverAccount_OwnerIdentityNumber(
                Transaction.TransactionStatus.PENDING_APPROVAL, identityNumber)) {
            log.warn("Müşteri hesap kapatma talebi bekleyen transfer nedeniyle reddedildi.");
            throw new BankOperationException("Müşterinin onay bekleyen transferleri var. Önce bu işlemler sonuçlandırılmalıdır.");
        }

        // Keep accounts and transaction history; repeated closure of an empty account is safe.
        accounts.forEach(acc -> acc.setActive(false));
        accountRepository.saveAll(accounts);
        log.info("Müşteri hesapları için pasife alma kaydı hazırlandı. accountCount={}", accounts.size());
    }
}
