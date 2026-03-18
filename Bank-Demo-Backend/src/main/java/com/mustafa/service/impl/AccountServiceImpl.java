package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.CreateAccountRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.Company;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.IAccountService;
import com.mustafa.util.AccountUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 LOGGER AKTİF
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements IAccountService {

    private final IAccountRepository accountRepository;
    private final IAppUserRepository appUserRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final ICompanyRepository companyRepository;

    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 KVKK Maskeleme
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    private AppUser getAuthenticatedAppUser() {
        String identityNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new RuntimeException("Yetkili kullanıcı bulunamadı!"));
    }

    private String getOwnerName(AppUser appUser) {
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            return retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(r -> r.getFirstName() + " " + r.getLastName()).orElse("Bilinmeyen Birey");
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            return companyRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(Company::getCompanyName).orElse("Bilinmeyen Şirket");
        }
        return "Sistem Yöneticisi";
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        AppUser currentUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(currentUser.getIdentityNumber());

        log.info("Yeni hesap açılış işlemi başlatıldı. Kullanıcı: {}, Döviz: {}", maskedId, request.getCurrency());

        if (currentUser.getStatus() != AppUser.ApprovalStatus.APPROVED) {
            log.warn("Hesap açılışı reddedildi: Kullanıcı ({}) henüz onaylanmamış (PENDING).", maskedId);
            throw new BankOperationException("Hesabınız onaylı olmadığı için yeni banka hesabı açamazsınız.");
        }

        String accountNumber;
        String iban;
        do {
            accountNumber = AccountUtils.generateAccountNumber();
            iban = AccountUtils.generateIban(accountNumber);
        } while (accountRepository.existsByAccountNumber(accountNumber) || accountRepository.existsByIban(iban));

        Account newAccount = Account.builder()
                .appUser(currentUser)
                .currency(request.getCurrency())
                .accountNumber(accountNumber)
                .iban(iban)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();

        Account savedAccount = accountRepository.save(newAccount);
        log.info("Hesap başarıyla oluşturuldu. Hesap No: {}, IBAN: {}, Sahibi: {}", accountNumber, iban, maskedId);

        // Müşteriye "Hesabınız açıldı" maili/SMS'i atılması için arka plana iş bırakıyoruz.
// YENİ HALİ: Yeni Kasa Açılış DTO'su
        NotificationMessage openAccountMessage = NotificationMessage.builder()
                .destination(currentUser.getIdentityNumber()) // İleride e-posta servisine bağlanabilir
                .subject("🎉 Yeni Banka Hesabınız Açıldı")
                .content(String.format("Sayın kullanıcımız, bankamız nezdinde %s döviz cinsinden %s numaralı (IBAN: %s) yeni hesabınız başarıyla açılmıştır. İyi günlerde kullanın.",
                        request.getCurrency(), accountNumber, iban))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(openAccountMessage);
        return mapToResponse(savedAccount);
    }

    @Override
    public List<AccountResponse> getMyAccounts() {
        AppUser currentUser = getAuthenticatedAppUser();
        log.info("Hesap listesi çekiliyor. Kullanıcı: {}", maskIdentity(currentUser.getIdentityNumber()));

        List<Account> accounts = accountRepository.findByAppUserId(currentUser.getId());
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        AppUser currentUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(currentUser.getIdentityNumber());

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Sorgulanan hesap bulunamadı! Hesap No: {}", accountNumber);
                    return new BankOperationException("Hesap bulunamadı!");
                });

        if (!account.getAppUser().getId().equals(currentUser.getId())) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Kullanıcı ({}), başkasına ait {} numaralı hesaba erişmeye çalıştı!", maskedId, accountNumber);
            throw new BankOperationException("Sadece kendi hesap detaylarınızı görüntüleyebilirsiniz!");
        }

        return mapToResponse(account);
    }

    @Override
    @Transactional
    public void deleteAccount(String accountNumber) {
        AppUser currentUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(currentUser.getIdentityNumber());

        log.info("Hesap kapatma işlemi başlatıldı. Hesap No: {}, Kullanıcı: {}", accountNumber, maskedId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankOperationException("Hesap bulunamadı!"));

        if (!account.isActive()) {
            log.warn("Hesap kapatma iptali: {} numaralı hesap zaten pasif durumda.", accountNumber);
            throw new BankOperationException("Bu hesap zaten kapatılmış!");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Hesap kapatma reddedildi: {} numaralı hesapta {} {} bakiye bulunuyor.", accountNumber, account.getBalance(), account.getCurrency());
            throw new BankOperationException("İçerisinde bakiye bulunan hesap kapatılamaz.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        if (!isAdmin) {
            if (!account.getAppUser().getId().equals(currentUser.getId())) {
                log.warn("🚨 YETKİSİZ İŞLEM! Kullanıcı ({}), başkasına ait {} numaralı hesabı kapatmaya çalıştı!", maskedId, accountNumber);
                throw new BankOperationException("Sadece kendi hesaplarınızı kapatabilirsiniz!");
            }
            if (currentUser.getStatus() != AppUser.ApprovalStatus.APPROVED) {
                log.warn("Hesap kapatma reddedildi: Kullanıcı ({}) onaylı değil.", maskedId);
                throw new BankOperationException("Hesabınız onaylı olmadığı için hesap kapatma işlemi gerçekleştiremezsiniz.");
            }
        } else {
            log.info("Admin yetkisiyle hesap kapatma işlemi uygulanıyor. Hesap No: {}", accountNumber);
        }

        account.setActive(false);
        accountRepository.save(account);
        log.info("Hesap başarıyla pasife alındı (Kapatıldı). Hesap No: {}", accountNumber);

        // YENİ HALİ: Hesap Kapatma DTO'su
        NotificationMessage closeAccountMessage = NotificationMessage.builder()
                .destination(currentUser.getIdentityNumber())
                .subject("Hesap Kapatma İşlemi Başarılı")
                .content(String.format("Bankamızdaki %s numaralı %s hesabınız isteğiniz üzerine / güvenlik kuralları gereği başarıyla kapatılmış (pasife alınmış) durumdadır.",
                        accountNumber, account.getCurrency()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL) // veya SMS
                .build();

        rabbitPublisher.sendNotification(closeAccountMessage);}

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .iban(account.getIban())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .isActive(account.isActive())
                .ownerName(getOwnerName(account.getAppUser()))
                .identityNumber(account.getAppUser().getIdentityNumber())
                .build();
    }
}