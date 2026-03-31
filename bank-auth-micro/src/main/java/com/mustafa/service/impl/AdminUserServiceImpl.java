package com.mustafa.service.impl;

import com.mustafa.client.CompanyServiceClient;
import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.IAdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final IAppUserRepository appUserRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final CompanyServiceClient companyServiceClient;
    private final RabbitMQPublisher rabbitPublisher;

    @Override
    public List<UserProfileResponse> getAllCustomers() {
        return appUserRepository.findAll().stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserProfileResponse updateCustomer(String identityNumber, UpdateProfileRequest request) {
        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new BankOperationException("Güncellenecek kullanıcı bulunamadı!"));

        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            RetailCustomer retail = retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber)
                    .orElseThrow(() -> new BankOperationException("Bireysel profil bulunamadı!"));

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
            log.info("Admin İşlemi: Bireysel müşteri ({}) güncellendi.", identityNumber);

        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            CompanySyncRequest syncRequest = CompanySyncRequest.builder()
                    .companyName(request.getProfileName())
                    .contactEmail(request.getEmail())
                    .companyIdentityNumber(identityNumber)
                    .build();
            try {
                companyServiceClient.updateCompanyInfo(identityNumber, syncRequest);
                log.info("Admin İşlemi: Kurumsal müşteri ({}) bilgileri Kurumsal Serviste güncellendi.", identityNumber);
            } catch (Exception e) {
                log.error("Kurumsal servis güncellenemedi: {}", e.getMessage());
                throw new BankOperationException("Kurumsal servis ile bağlantı kurulamadı, güncelleme iptal edildi!");
            }
        }

        return UserProfileResponse.builder()
                .identityNumber(appUser.getIdentityNumber())
                .profileName(request.getProfileName() != null ? request.getProfileName() : getOwnerName(appUser))
                .email(request.getEmail() != null ? request.getEmail() : getOwnerEmail(appUser))
                .role(appUser.getRole().name())
                .status(appUser.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public void updateCustomerStatus(String identityNumber, String status) {
        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new BankOperationException("Kullanıcı bulunamadı!"));
        appUser.setStatus(AppUser.ApprovalStatus.valueOf(status.toUpperCase()));
        appUserRepository.save(appUser);
        log.info("Admin İşlemi: {} kimlik numaralı kullanıcının statüsü {} olarak güncellendi.", identityNumber, status);
    }

    @Override
    @Transactional
    public void deleteCustomer(String identityNumber) {
        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new BankOperationException("Kullanıcı bulunamadı!"));

        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber)
                    .ifPresent(retailCustomer -> {
                        retailCustomerRepository.delete(retailCustomer);
                        log.info("Admin İşlemi: {} numaralı bireysel profil silindi.", identityNumber);
                    });
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            try {
                companyServiceClient.deleteCompanyInfo(identityNumber);
                log.info("Admin İşlemi: {} numaralı şirket Kurumsal Servisten silindi.", identityNumber);
            } catch (Exception e) {
                log.error("Kurumsal servis silinirken hata oluştu: {}", e.getMessage());
                throw new BankOperationException("Kurumsal servisteki veriler silinemedi, işlem iptal edildi!");
            }
        }

        NotificationMessage deleteSignal = NotificationMessage.builder()
                .destination("admin@bank.com")
                .subject("USER_DELETED_CLEANUP")
                .identityNumber(identityNumber)
                .content(identityNumber)
                .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT)
                .build();
        rabbitPublisher.sendNotification(deleteSignal);

        appUserRepository.delete(appUser);
        log.info("Admin İşlemi: {} sistemden (Giriş Yetkisi) tamamen temizlendi.", identityNumber);
    }

    private String getOwnerName(AppUser appUser) {
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            return retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(r -> r.getFirstName() + " " + r.getLastName()).orElse("Bilinmeyen Birey");
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            return "Kurumsal Müşteri (" + appUser.getIdentityNumber() + ")";
        }
        return "Sistem Yöneticisi";
    }

    private String getOwnerEmail(AppUser appUser) {
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            return retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(RetailCustomer::getEmail).orElse("");
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            return "kurumsal@sistem.com";
        }
        return "admin@bank.com";
    }

    private UserProfileResponse mapToUserProfileResponse(AppUser appUser) {
        return UserProfileResponse.builder()
                .identityNumber(appUser.getIdentityNumber())
                .profileName(getOwnerName(appUser))
                .email(getOwnerEmail(appUser))
                .role(appUser.getRole().name())
                .status(appUser.getStatus().name())
                .build();
    }
}