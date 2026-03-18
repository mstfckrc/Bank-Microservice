package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.Company;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 LOGGER AKTİF
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final IAppUserRepository appUserRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final ICompanyRepository companyRepository;
    private final IAccountRepository accountRepository;
    private final ITransactionRepository transactionRepository;

    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 KVKK Maskeleme Kalkanı
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    // Ortak Yardımcı Metot: Şirket veya Bireyin ismini bulur
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

    // Ortak Yardımcı Metot: Email adresini bulur
    private String getOwnerEmail(AppUser appUser) {
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            return retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(RetailCustomer::getEmail).orElse("");
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            return companyRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(Company::getContactEmail).orElse("");
        }
        return "admin@bank.com";
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        log.info("Admin İşlemi: Sistemdeki tüm hesaplar veritabanından çekiliyor.");
        return accountRepository.findAll().stream()
                .map(account -> AccountResponse.builder()
                        .id(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .currency(account.getCurrency())
                        .isActive(account.isActive())
                        .ownerName(getOwnerName(account.getAppUser()))
                        .identityNumber(account.getAppUser().getIdentityNumber())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getCustomerAccounts(String identityNumber) {
        String maskedId = maskIdentity(identityNumber);

        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("Admin İşlemi Başarısız: Hesapları sorgulanan müşteri ({}) bulunamadı!", maskedId);
                    return new BankOperationException("Kullanıcı bulunamadı!");
                });

        log.info("Admin İşlemi: Müşterinin ({}) hesap listesi başarıyla getirildi.", maskedId);
        return appUser.getAccounts().stream()
                .map(account -> AccountResponse.builder()
                        .id(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .currency(account.getCurrency())
                        .isActive(account.isActive())
                        .ownerName(getOwnerName(appUser))
                        .identityNumber(appUser.getIdentityNumber())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfileResponse> getAllCustomers() {
        log.info("Admin İşlemi: Sistemdeki tüm müşteri profilleri veritabanından çekiliyor.");
        return appUserRepository.findAll().stream()
                .map(appUser -> UserProfileResponse.builder()
                        .identityNumber(appUser.getIdentityNumber())
                        .profileName(getOwnerName(appUser))
                        .email(getOwnerEmail(appUser))
                        .role(appUser.getRole().name())
                        .status(appUser.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCustomer(String identityNumber) {
        String maskedId = maskIdentity(identityNumber);

        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("Admin İşlemi Başarısız: Silinmek istenen müşteri ({}) sistemde yok!", maskedId);
                    return new BankOperationException("Silinecek kullanıcı bulunamadı!");
                });

        appUserRepository.delete(appUser);
        log.info("Admin İşlemi Başarılı: Müşteri ({}) ve bağlı tüm hesapları sistemden tamamen (Cascade) silindi.", maskedId);

        // YENİ HALİ: Müşteri Silinme DTO'su
        NotificationMessage deleteMessage = NotificationMessage.builder()
                .destination("admin@bank.com") // Sistem yöneticisi e-postası veya log kanalı
                .subject("ADMİN İŞLEMİ: Müşteri Kaydı Silindi")
                .content(String.format("Sistem yöneticisi tarafından %s kimlik numaralı müşterinin kaydı ve bağlı tüm hesapları (Cascade) tamamen silinmiştir.", maskedId))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT)
                .build();

        rabbitPublisher.sendNotification(deleteMessage);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCustomer(String identityNumber, UpdateProfileRequest request) {
        String maskedId = maskIdentity(identityNumber);

        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("Admin İşlemi Başarısız: Profili güncellenmek istenen müşteri ({}) bulunamadı!", maskedId);
                    return new BankOperationException("Güncellenecek kullanıcı bulunamadı!");
                });

        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            RetailCustomer retail = retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber).get();

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                retail.setEmail(request.getEmail());
            }

            if (request.getProfileName() != null && !request.getProfileName().isBlank()) {
                String fullName = request.getProfileName().trim();
                int lastSpaceIndex = fullName.lastIndexOf(" ");

                if (lastSpaceIndex == -1) {
                    retail.setFirstName(fullName);
                    retail.setLastName("");
                } else {
                    retail.setFirstName(fullName.substring(0, lastSpaceIndex).trim());
                    retail.setLastName(fullName.substring(lastSpaceIndex + 1).trim());
                }
            }
            retailCustomerRepository.save(retail);

        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            Company company = companyRepository.findByAppUser_IdentityNumber(identityNumber).get();

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                company.setContactEmail(request.getEmail());
            }

            if (request.getProfileName() != null && !request.getProfileName().isBlank()) {
                company.setCompanyName(request.getProfileName().trim());
            }
            companyRepository.save(company);
        }

        log.info("Admin İşlemi Başarılı: Müşteri ({}) profil bilgileri güncellendi.", maskedId);

// YENİ HALİ: Profil Güncelleme DTO'su
        NotificationMessage updateMessage = NotificationMessage.builder()
                .destination(getOwnerEmail(appUser)) // Müşterinin güncel e-postası
                .subject("Profil Bilgileriniz Güncellendi (Sistem Yöneticisi)")
                .content("Müşteri profil bilgileriniz sistem yöneticisi tarafından güncellenmiştir. Herhangi bir sorunuz varsa lütfen müşteri hizmetleri ile iletişime geçiniz.")
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(updateMessage);
        return UserProfileResponse.builder()
                .identityNumber(appUser.getIdentityNumber())
                .profileName(getOwnerName(appUser))
                .email(getOwnerEmail(appUser))
                .role(appUser.getRole().name())
                .status(appUser.getStatus().name())
                .build();
    }

    @Override
    public List<TransactionResponse> getAccountTransactions(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Admin İşlemi Başarısız: İşlem geçmişi istenen hesap ({}) bulunamadı!", accountNumber);
                    return new BankOperationException("Hesap bulunamadı!");
                });

        log.info("Admin İşlemi: Hesabın ({}) geçmiş transferleri getiriliyor.", accountNumber);

        List<Transaction> transactions = transactionRepository
                .findBySenderAccountIdOrReceiverAccountIdOrderByTransactionDateDesc(account.getId(), account.getId());

        return transactions.stream().map(transaction -> TransactionResponse.builder()
                .referenceNo(transaction.getReferenceNo())
                .amount(transaction.getAmount())
                .convertedAmount(transaction.getConvertedAmount() != null ? transaction.getConvertedAmount() : transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .senderAccountId(transaction.getSenderAccount() != null ? transaction.getSenderAccount().getId() : null)
                .receiverAccountId(transaction.getReceiverAccount() != null ? transaction.getReceiverAccount().getId() : null)
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse openAccountForCustomer(String identityNumber, OpenAccountRequest request) {
        String maskedId = maskIdentity(identityNumber);

        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("Admin İşlemi Başarısız: Hesap açılacak müşteri ({}) bulunamadı!", maskedId);
                    return new BankOperationException("Kullanıcı bulunamadı!");
                });

        Account.Currency accountCurrency;
        try {
            if (request.getCurrency() == null || request.getCurrency().isBlank()) throw new IllegalArgumentException();
            accountCurrency = Account.Currency.valueOf(request.getCurrency().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Admin İşlemi Başarısız: Geçersiz para birimi girişi yapıldı. Girilen: {}", request.getCurrency());
            throw new BankOperationException("Geçersiz para birimi! Sadece TRY, USD veya EUR desteklenmektedir.");
        }

        String generatedAccountNumber = String.valueOf((long) (Math.random() * 9000000000L) + 1000000000L);
        String generatedIban = "TR" + "00000" + generatedAccountNumber + "000000001";

        Account newAccount = Account.builder()
                .appUser(appUser)
                .accountNumber(generatedAccountNumber)
                .iban(generatedIban)
                .balance(java.math.BigDecimal.ZERO)
                .isActive(true)
                .currency(accountCurrency)
                .build();

        Account savedAccount = accountRepository.save(newAccount);
        log.info("Admin İşlemi Başarılı: Müşteriye ({}) yeni hesap (No: {}, Döviz: {}) açıldı.", maskedId, generatedAccountNumber, accountCurrency);

// YENİ HALİ: Yeni Kasa Açılış DTO'su
        NotificationMessage accountMessage = NotificationMessage.builder()
                .destination(getOwnerEmail(appUser))
                .subject("Yeni Banka Hesabınız Açıldı")
                .content(String.format("Sayın %s, sistem yöneticisi tarafından adınıza %s döviz cinsinden %s numaralı yeni bir banka hesabı (kasa) başarıyla açılmıştır.",
                        getOwnerName(appUser), accountCurrency.name(), generatedAccountNumber))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(accountMessage);
        return AccountResponse.builder()
                .id(savedAccount.getId())
                .accountNumber(savedAccount.getAccountNumber())
                .iban(savedAccount.getIban())
                .balance(savedAccount.getBalance())
                .currency(savedAccount.getCurrency())
                .isActive(savedAccount.isActive())
                .ownerName(getOwnerName(appUser))
                .identityNumber(appUser.getIdentityNumber())
                .build();
    }

    @Transactional
    @Override
    public void updateCustomerStatus(String identityNumber, String status) {
        String maskedId = maskIdentity(identityNumber);

        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("Admin İşlemi Başarısız: Onay durumu değiştirilecek müşteri ({}) bulunamadı!", maskedId);
                    return new BankOperationException("Kullanıcı bulunamadı!");
                });

        appUser.setStatus(AppUser.ApprovalStatus.valueOf(status.toUpperCase()));
        appUserRepository.save(appUser);

        log.info("Admin İşlemi Başarılı: Müşterinin ({}) sistem durumu [{}] olarak güncellendi.", maskedId, status.toUpperCase());

// YENİ HALİ: Onay/Ret Durumu DTO'su
        NotificationMessage statusMessage = NotificationMessage.builder()
                .destination(getOwnerEmail(appUser))
                .subject("Hesap Durumunuz Güncellendi")
                .content(String.format("Sayın %s, bankacılık sistemi hesap onay durumunuz sistem yöneticisi tarafından [%s] olarak güncellenmiştir.",
                        getOwnerName(appUser), status.toUpperCase()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(statusMessage);    }
}