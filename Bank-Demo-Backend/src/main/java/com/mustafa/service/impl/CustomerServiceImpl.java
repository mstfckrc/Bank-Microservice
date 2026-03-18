package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.ChangePasswordRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.Company;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.ICustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j // 🚀 LOGGER AKTİF
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements ICustomerService {

    private final IAppUserRepository appUserRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final ICompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 KVKK Maskeleme Kalkanı
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    private AppUser getAuthenticatedAppUser() {
        String identityNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.error("Kimlik doğrulama hatası: Context'te bulunan TC/Vergi No ({}) veritabanında yok!", maskIdentity(identityNumber));
                    return new BankOperationException("Kullanıcı bulunamadı!");
                });
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        AppUser appUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(appUser.getIdentityNumber());
        log.info("Profil güncelleme işlemi başlatıldı. Kullanıcı: {}", maskedId);

        String profileName = "";
        String email = "";

        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            RetailCustomer retail = retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber()).get();

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                retail.setEmail(request.getEmail());
            }

            if (request.getProfileName() != null && request.getProfileName().trim().length() >= 3) {
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

            profileName = (retail.getFirstName() + " " + retail.getLastName()).trim();
            email = retail.getEmail();

        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            Company company = companyRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber()).get();

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                company.setContactEmail(request.getEmail());
            }

            if (request.getProfileName() != null && request.getProfileName().trim().length() >= 3) {
                company.setCompanyName(request.getProfileName().trim());
            }
            companyRepository.save(company);

            profileName = company.getCompanyName();
            email = company.getContactEmail();
        }

        log.info("Profil başarıyla güncellendi. Kullanıcı: {}", maskedId);

        NotificationMessage profileMessage = NotificationMessage.builder()
                .destination(email) // Müşterinin yeni e-posta adresi
                .subject("Profil Bilgileriniz Güncellendi")
                .content("Müşteri profil bilgileriniz başarıyla güncellenmiştir. Yeni iletişim adresiniz: " + email)
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(profileMessage);
        return UserProfileResponse.builder()
                .identityNumber(appUser.getIdentityNumber())
                .profileName(profileName)
                .email(email)
                .role(appUser.getRole().name())
                .status(appUser.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AppUser appUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(appUser.getIdentityNumber());
        log.info("Şifre değiştirme talebi alındı. Kullanıcı: {}", maskedId);

        if (!passwordEncoder.matches(request.getOldPassword(), appUser.getPassword())) {
            log.warn("Şifre değiştirme başarısız: Eski şifre hatalı! Kullanıcı: {}", maskedId);
            throw new BankOperationException("Eski şifreniz hatalı!");
        }
        if (request.getNewPassword().length() < 6) {
            log.warn("Şifre değiştirme başarısız: Yeni şifre kurallara uymuyor (Çok kısa). Kullanıcı: {}", maskedId);
            throw new BankOperationException("Yeni şifreniz en az 6 karakter olmalıdır!");
        }

        appUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(appUser);

        // YENİ HALİ: Siber Güvenlik Alarm DTO'su
        NotificationMessage securityMessage = NotificationMessage.builder()
                .destination(appUser.getIdentityNumber()) // SMS atılacağı için hedef olarak kimlik no veya telefon verilebilir
                .subject("🚨 Güvenlik Uyarısı: Şifreniz Değiştirildi")
                .content("Dijital bankacılık şifreniz az önce değiştirildi. Bu işlemi siz gerçekleştirmediyseniz lütfen acilen müşteri hizmetlerimizle iletişime geçiniz!")
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.SMS) // Acil durum olduğu için SMS!
                .build();

        rabbitPublisher.sendNotification(securityMessage);
    }

    @Override
    public UserProfileResponse getMyProfile() {
        AppUser appUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(appUser.getIdentityNumber());
        log.info("Profil detayları getiriliyor. Kullanıcı: {}", maskedId);

        String profileName = "";
        String email = "";

        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            RetailCustomer retail = retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber()).get();
            profileName = retail.getFirstName() + " " + retail.getLastName();
            email = retail.getEmail();
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            Company company = companyRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber()).get();
            profileName = company.getCompanyName();
            email = company.getContactEmail();
        } else {
            profileName = "Sistem Yöneticisi";
            email = "admin@bank.com";
        }

        return UserProfileResponse.builder()
                .identityNumber(appUser.getIdentityNumber())
                .profileName(profileName)
                .email(email)
                .role(appUser.getRole().name())
                .status(appUser.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public void appealRejection() {
        AppUser appUser = getAuthenticatedAppUser();
        String maskedId = maskIdentity(appUser.getIdentityNumber());
        log.info("Yeniden değerlendirme (appeal) talebi alındı. Kullanıcı: {}", maskedId);

        if (appUser.getStatus() != AppUser.ApprovalStatus.REJECTED) {
            log.warn("İtiraz reddedildi: Kullanıcı ({}) durumu REJECTED değil (Mevcut: {}).", maskedId, appUser.getStatus());
            throw new BankOperationException("Sadece reddedilen hesaplar yeniden değerlendirme talebinde bulunabilir.");
        }

        appUser.setStatus(AppUser.ApprovalStatus.PENDING);
        appUserRepository.save(appUser);
        log.info("Yeniden değerlendirme talebi başarıyla işleme alındı. Durum PENDING yapıldı. Kullanıcı: {}", maskedId);
        // YENİ HALİ: Admin İtiraz (Appeal) DTO'su
        NotificationMessage appealMessage = NotificationMessage.builder()
                .destination("admin@bank.com") // Doğrudan admin yetkilisine gidiyor
                .subject("Yeni Hesap Onay İtirazı (Appeal)")
                .content(String.format("Dikkat: %s kimlik numaralı müşteri, reddedilen hesabı için yeniden değerlendirme talep etmektedir. Lütfen paneli kontrol edin.", maskedId))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT) // Sistem Alarmı
                .build();

        rabbitPublisher.sendNotification(appealMessage);
    }
}