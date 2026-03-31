package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.CreateAccountRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.service.IAccountService;
import com.mustafa.util.AccountUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements IAccountService {

    private final IAccountRepository accountRepository;
    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 YENİ MİMARİ: Statüyü okumak için
    private final HttpServletRequest httpRequest;

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    // 🚀 YENİ MİMARİ: Gateway'in getirdiği X-User-Status başlığını okuyan metot
    private void checkUserApprovalStatus() {
        String status = httpRequest.getHeader("X-User-Status");
        if (status == null || !status.equals("APPROVED")) {
            throw new BankOperationException("Hesabınız onaylı olmadığı (PENDING) için yeni banka hesabı açamaz veya kapatamazsınız!");
        }
    }

    // 🚀 YENİ MİMARİ: Karargah isimleri unuttu! Şimdilik maskeli TC dönüyoruz.
    // (Gerekirse ileride Kimlik Üssüne Feign Client ile bağlanıp gerçek ismini sorabiliriz)
    private String getOwnerName(String identityNumber) {
        return "Müşteri (" + maskIdentity(identityNumber) + ")";
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);

        log.info("Yeni hesap açılış işlemi başlatıldı. Kullanıcı: {}, Döviz: {}", maskedId, request.getCurrency());

        // 1. STATÜ KONTROLÜ (Veritabanından değil, Havadan / Header'dan geliyor)
        checkUserApprovalStatus();

        String accountNumber;
        String iban;
        do {
            accountNumber = AccountUtils.generateAccountNumber();
            iban = AccountUtils.generateIban(accountNumber);
        } while (accountRepository.existsByAccountNumber(accountNumber) || accountRepository.existsByIban(iban));

        Account newAccount = Account.builder()
                .ownerIdentityNumber(currentIdentity) // 🚀 Sadece TC!
                .currency(request.getCurrency())
                .accountNumber(accountNumber)
                .iban(iban)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();

        Account savedAccount = accountRepository.save(newAccount);
        log.info("Hesap başarıyla oluşturuldu. Hesap No: {}, IBAN: {}, Sahibi: {}", accountNumber, iban, maskedId);

        NotificationMessage openAccountMessage = NotificationMessage.builder()
                .destination(currentIdentity)
                .subject("🎉 Yeni Banka Hesabınız Açıldı")
                .content(String.format("Sayın kullanıcımız, bankamız nezdinde %s döviz cinsinden %s numaralı (IBAN: %s) yeni hesabınız başarıyla açılmıştır.",
                        request.getCurrency(), accountNumber, iban))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(openAccountMessage);
        return mapToResponse(savedAccount);
    }

    @Override
    public List<AccountResponse> getMyAccounts() {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Hesap listesi çekiliyor. Kullanıcı: {}", maskIdentity(currentIdentity));

        List<Account> accounts = accountRepository.findByOwnerIdentityNumber(currentIdentity);
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankOperationException("Hesap bulunamadı!"));

        if (!account.getOwnerIdentityNumber().equals(currentIdentity)) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Kullanıcı ({}), başkasına ait {} numaralı hesaba erişmeye çalıştı!", maskedId, accountNumber);
            throw new BankOperationException("Sadece kendi hesap detaylarınızı görüntüleyebilirsiniz!");
        }

        return mapToResponse(account);
    }

    @Override
    @Transactional
    public void deleteAccount(String accountNumber) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);

        log.info("Hesap kapatma işlemi başlatıldı. Hesap No: {}, Kullanıcı: {}", accountNumber, maskedId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankOperationException("Hesap bulunamadı!"));

        if (!account.isActive()) {
            throw new BankOperationException("Bu hesap zaten kapatılmış!");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BankOperationException("İçerisinde bakiye bulunan hesap kapatılamaz.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            if (!account.getOwnerIdentityNumber().equals(currentIdentity)) {
                throw new BankOperationException("Sadece kendi hesaplarınızı kapatabilirsiniz!");
            }
            // 🚀 STATÜ KONTROLÜ
            checkUserApprovalStatus();
        }

        account.setActive(false);
        accountRepository.save(account);
        log.info("Hesap başarıyla pasife alındı (Kapatıldı). Hesap No: {}", accountNumber);

        NotificationMessage closeAccountMessage = NotificationMessage.builder()
                .destination(account.getOwnerIdentityNumber())
                .subject("Hesap Kapatma İşlemi Başarılı")
                .content(String.format("Bankamızdaki %s numaralı %s hesabınız başarıyla kapatılmış (pasife alınmış) durumdadır.",
                        accountNumber, account.getCurrency()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(closeAccountMessage);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .iban(account.getIban())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .isActive(account.isActive())
                .ownerName(getOwnerName(account.getOwnerIdentityNumber())) // Placeholder dönüyor
                .identityNumber(account.getOwnerIdentityNumber())
                .build();
    }
}