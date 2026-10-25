package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.ICustomerService;
import com.mustafa.security.BankUserAccess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements ICustomerService {

    private final IAppUserRepository appUserRepository;
    private final BankUserAccess bankUserAccess;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final RabbitMQPublisher rabbitPublisher;

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    // Resolve the verified subject; a request header is not authentication.
    private AppUser getAuthenticatedAppUser() {
        return bankUserAccess.currentUser();
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
            log.warn("Profil güncelleme reddedildi: Kurumsal müşteri ({}) bilgileri Karargahtan güncellenemez.", maskedId);
            throw new BankOperationException("Kurumsal profil bilgileri (Şirket Adı, E-posta) sadece Kurumsal Yönetim modülünden güncellenebilir!");
        }

        log.info("Profil başarıyla güncellendi. Kullanıcı: {}", maskedId);

        NotificationMessage profileMessage = NotificationMessage.builder()
                .destination(email)
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

    // 🚀 SİLİNDİ: changePassword metodu tamamen kaldırıldı! Şifre işlemleri Keycloak'tadır.

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
            profileName = "Kurumsal Müşteri (" + appUser.getIdentityNumber() + ")";
            email = "kurumsal@sistem.com";
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

        NotificationMessage appealMessage = NotificationMessage.builder()
                .destination("admin@bank.com")
                .subject("Yeni Hesap Onay İtirazı (Appeal)")
                .content(String.format("Dikkat: %s kimlik numaralı müşteri, reddedilen hesabı için yeniden değerlendirme talep etmektedir. Lütfen paneli kontrol edin.", maskedId))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT)
                .build();

        rabbitPublisher.sendNotification(appealMessage);
    }
}
