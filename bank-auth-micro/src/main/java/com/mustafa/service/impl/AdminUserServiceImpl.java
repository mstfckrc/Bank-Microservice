package com.mustafa.service.impl;

import com.mustafa.client.BackendServiceClient;
import com.mustafa.client.CompanyServiceClient;
import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.dto.request.AccessReviewDecisionRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.AccessRestrictionAudit;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.exception.AccessRestrictionConflictException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.IAccessRestrictionAuditRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.security.BankUserAccess;
import com.mustafa.service.IAdminUserService;
import com.mustafa.validation.ProfileUpdateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.ws.rs.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final IAppUserRepository appUserRepository;
    private final IAccessRestrictionAuditRepository accessRestrictionAuditRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final CompanyServiceClient companyServiceClient;
    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 YENİ EKLENDİ: Karargaha giden doğrudan telsiz hattı
    private final BackendServiceClient backendServiceClient;
    private final BankUserAccess bankUserAccess;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public List<UserProfileResponse> getAllCustomers() {
        return appUserRepository.findAll().stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserProfileResponse updateCustomer(String identityNumber, UpdateProfileRequest request) {
        AppUser appUser = appUserRepository.findByIdentityNumberForUpdate(identityNumber)
                .orElseThrow(() -> new BankOperationException("Güncellenecek kullanıcı bulunamadı!"));
        ProfileUpdateValidator.validate(request, appUser.getRole());

        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER || appUser.getRole() == AppUser.Role.ADMIN) {
            RetailCustomer retail = retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber)
                    .orElseThrow(() -> new BankOperationException("Düzenlenebilir yerel profil bulunamadı. Yönetici profilini merkezi kimlik yönetiminden güncelleyin."));

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                retail.setEmail(request.getEmail().trim());
            }

            if (request.getFirstName() != null) {
                retail.setFirstName(request.getFirstName().trim());
                retail.setLastName(request.getLastName().trim());
            }
            retailCustomerRepository.save(retail);
            log.info("Admin profil güncelleme kaydı hazırlandı. targetAppUserId={}", appUser.getId());

        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            CompanySyncRequest syncRequest = CompanySyncRequest.builder()
                    .companyName(request.getProfileName() == null ? null : request.getProfileName().trim())
                    .contactEmail(request.getEmail() == null ? null : request.getEmail().trim())
                    .companyIdentityNumber(identityNumber)
                    .build();
            try {
                companyServiceClient.updateCompanyInfo(identityNumber, syncRequest);
                log.info("Admin kurumsal profil güncellemesi uzak serviste tamamlandı. targetAppUserId={}", appUser.getId());
            } catch (Exception e) {
                log.error("Kurumsal profil güncellenemedi. errorType={}", e.getClass().getSimpleName());
                throw new BankOperationException("Kurumsal servis ile bağlantı kurulamadı, güncelleme iptal edildi!");
            }
        }

        return mapToUserProfileResponse(appUser);
    }

    @Override
    @Transactional
    public void updateCustomerStatus(String identityNumber, String status) {
        AppUser actor = bankUserAccess.currentUser();
        AppUser appUser = appUserRepository.findByIdentityNumberForUpdate(identityNumber)
                .orElseThrow(() -> new BankOperationException("Kullanıcı bulunamadı!"));
        if (actor.getId().equals(appUser.getId())) {
            throw new AccessRestrictionConflictException("SELF_ACCOUNT_STATUS_CHANGE_FORBIDDEN",
                    "Yönetici kendi hesap durumunu bu işlemle değiştiremez.");
        }
        if (appUser.getRole() == AppUser.Role.ADMIN) {
            throw new AccessRestrictionConflictException("ADMIN_ACCOUNT_PROTECTED",
                    "Yönetici hesabı müşteri onay işlemiyle değiştirilemez.");
        }
        AppUser.ApprovalStatus requestedStatus = AppUser.ApprovalStatus.valueOf(status.toUpperCase());
        if (appUser.getStatus() == AppUser.ApprovalStatus.APPROVED
                && requestedStatus != AppUser.ApprovalStatus.APPROVED) {
            throw new AccessRestrictionConflictException("APPROVED_ACCOUNT_STATUS_CHANGE_FORBIDDEN",
                    "Onaylı müşteri kaydının onay durumu değiştirilemez. Erişimi durdurmak için erişim kısıtlamasını kullanın.");
        }

        appUser.setStatus(requestedStatus);
        appUserRepository.save(appUser);
        log.info("Müşteri başvuru durumu değişti. actorAppUserId={} targetAppUserId={} status={}",
                actor.getId(), appUser.getId(), appUser.getStatus());
    }

    @Override
    @Transactional
    public void updateAccessRestriction(String identityNumber, boolean restricted) {
        AppUser actor = bankUserAccess.currentUser();
        AppUser target = appUserRepository.findByIdentityNumberForUpdate(identityNumber)
                .orElseThrow(() -> new BankOperationException("Kullanıcı bulunamadı!"));

        if (actor.getId().equals(target.getId())) {
            throw new AccessRestrictionConflictException("SELF_RESTRICTION_FORBIDDEN",
                    "Yönetici kendi erişim durumunu bu işlemle değiştiremez.");
        }
        if (target.getRole() == AppUser.Role.ADMIN) {
            throw new AccessRestrictionConflictException("ADMIN_ACCOUNT_PROTECTED",
                    "Yönetici hesabı müşteri erişim kısıtı işlemiyle değiştirilemez.");
        }

        AppUser.AccessRestrictionStatus previous = target.getAccessRestrictionStatus();
        AppUser.AccessRestrictionStatus resulting;
        AccessRestrictionAudit.Action action;

        if (restricted) {
            if (previous != AppUser.AccessRestrictionStatus.UNRESTRICTED) return;
            resulting = AppUser.AccessRestrictionStatus.RESTRICTED;
            action = AccessRestrictionAudit.Action.RESTRICTED;
        } else {
            if (previous == AppUser.AccessRestrictionStatus.UNRESTRICTED) return;
            resulting = AppUser.AccessRestrictionStatus.UNRESTRICTED;
            action = AccessRestrictionAudit.Action.RESTRICTION_LIFTED;
        }

        target.setAccessRestrictionStatus(resulting);
        appUserRepository.save(target);
        recordRestrictionAudit(target, actor, action, previous, resulting);
        log.info("Erişim kısıtı değişti. actorAppUserId={} targetAppUserId={} action={} state={}",
                actor.getId(), target.getId(), action, resulting);
    }

    @Override
    @Transactional
    public void decideAccessReview(String identityNumber, AccessReviewDecisionRequest.Decision decision) {
        AppUser actor = bankUserAccess.currentUser();
        AppUser target = appUserRepository.findByIdentityNumberForUpdate(identityNumber)
                .orElseThrow(() -> new BankOperationException("Kullanıcı bulunamadı!"));

        if (target.getRole() == AppUser.Role.ADMIN) {
            throw new AccessRestrictionConflictException("ADMIN_ACCOUNT_PROTECTED",
                    "Yönetici hesabı erişim incelemesine alınamaz.");
        }
        if (target.getAccessRestrictionStatus() != AppUser.AccessRestrictionStatus.REVIEW_PENDING) {
            throw new AccessRestrictionConflictException("ACCESS_REVIEW_NOT_PENDING",
                    "Bu hesap için bekleyen bir erişim inceleme talebi yok.");
        }

        AppUser.AccessRestrictionStatus previous = target.getAccessRestrictionStatus();
        AppUser.AccessRestrictionStatus resulting = decision == AccessReviewDecisionRequest.Decision.APPROVE
                ? AppUser.AccessRestrictionStatus.UNRESTRICTED
                : AppUser.AccessRestrictionStatus.REVIEW_REJECTED;
        AccessRestrictionAudit.Action action = decision == AccessReviewDecisionRequest.Decision.APPROVE
                ? AccessRestrictionAudit.Action.REVIEW_APPROVED
                : AccessRestrictionAudit.Action.REVIEW_REJECTED;

        target.setAccessRestrictionStatus(resulting);
        appUserRepository.save(target);
        recordRestrictionAudit(target, actor, action, previous, resulting);
        log.info("Erişim inceleme kararı kaydedildi. actorAppUserId={} targetAppUserId={} action={} state={}",
                actor.getId(), target.getId(), action, resulting);
    }

    private void recordRestrictionAudit(AppUser target, AppUser actor,
                                        AccessRestrictionAudit.Action action,
                                        AppUser.AccessRestrictionStatus previous,
                                        AppUser.AccessRestrictionStatus resulting) {
        accessRestrictionAuditRepository.save(AccessRestrictionAudit.builder()
                .targetAppUserId(target.getId())
                .actorAppUserId(actor.getId())
                .action(action)
                .previousState(previous)
                .resultingState(resulting)
                .occurredAt(Instant.now())
                .build());
    }

    @Override
    @Transactional
    public void deleteCustomer(String identityNumber) {
        AppUser actor = bankUserAccess.currentUser();
        if (actor.getRole() != AppUser.Role.ADMIN
                || actor.getAccessRestrictionStatus() != AppUser.AccessRestrictionStatus.UNRESTRICTED) {
            throw new AccessDeniedException("Customer deletion requires an unrestricted administrator.");
        }
        var target = appUserRepository.findByIdentityNumberForUpdate(identityNumber);
        if (target.isEmpty()) {
            log.info("Müşteri silme isteği tekrarlandı; yerel kayıt zaten bulunmuyor. actorAppUserId={}", actor.getId());
            return;
        }
        AppUser appUser = target.get();
        if (actor.getId().equals(appUser.getId())) {
            throw new AccessRestrictionConflictException("SELF_DELETE_FORBIDDEN",
                    "Yönetici kendi hesabını müşteri silme işlemiyle silemez.");
        }
        if (appUser.getRole() == AppUser.Role.ADMIN) {
            throw new AccessRestrictionConflictException("ADMIN_ACCOUNT_PROTECTED",
                    "Yönetici hesabı müşteri silme işlemiyle silinemez.");
        }
        if (appUser.getKeycloakId() == null || appUser.getKeycloakId().isBlank()) {
            throw new BankOperationException("Kullanıcının merkezi kimlik eşlemesi eksik; silmeden önce yönetici incelemesi gerekiyor.");
        }

        // Core validates balances and pending transfers before any profile is removed.
        try {
            backendServiceClient.deleteCustomerAccounts(identityNumber);
        } catch (Exception e) {
            log.warn("Müşteri silme Core adımında durdu. targetAppUserId={} errorType={}",
                    appUser.getId(), e.getClass().getSimpleName());
            throw new BankOperationException("Banka hesapları kapatılamadı. Bakiye ve onay bekleyen transferleri kontrol edin; servis hatasında işlemi tekrar deneyin.");
        }

        if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            try {
                companyServiceClient.deleteCompanyInfo(identityNumber);
            } catch (Exception e) {
                log.error("Müşteri silme Corporate adımında durdu. targetAppUserId={} errorType={}",
                        appUser.getId(), e.getClass().getSimpleName());
                throw new BankOperationException("Şirket kaydı silinemedi. Banka hesapları kapatılmış olabilir; sorun giderildikten sonra silme işlemini tekrar deneyin.");
            }
        }

        // Flush local constraints before removing the external identity; local work is still uncommitted.
        try {
            retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber)
                    .ifPresent(retailCustomerRepository::delete);
            appUserRepository.delete(appUser);
            appUserRepository.flush();
        } catch (RuntimeException e) {
            log.error("Müşteri silme yerel veritabanı adımında durdu. targetAppUserId={} errorType={}",
                    appUser.getId(), e.getClass().getSimpleName());
            throw new BankOperationException("Yerel kullanıcı kaydı silinemedi. Önceki adımlar tamamlanmış olabilir; sorun giderildikten sonra işlemi tekrar deneyin.");
        }

        try {
            keycloak.realm(realm).users().get(appUser.getKeycloakId()).remove();
        } catch (NotFoundException e) {
            log.info("Silinecek merkezi kimlik zaten bulunmuyor. targetAppUserId={}", appUser.getId());
        } catch (RuntimeException e) {
            log.error("Müşteri silme Keycloak adımında durdu. targetAppUserId={} errorType={}",
                    appUser.getId(), e.getClass().getSimpleName());
            throw new BankOperationException("Merkezi giriş yetkisi kaldırılamadı. Önceki adımlar tamamlanmış olabilir; bağlantı düzeldiğinde silme işlemini tekrar deneyin.");
        }

        log.info("Müşteri silme adımları tamamlandı; yerel commit bekleniyor. actorAppUserId={} targetAppUserId={}",
                actor.getId(), appUser.getId());
    }

    private String getOwnerName(AppUser appUser) {
        // 🚀 DÜZELTİLDİ: Admin de olsan Bireysel Müşteri de olsan, profilin Retail tablosundaysa oradan çekecek!
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER || appUser.getRole() == AppUser.Role.ADMIN) {
            return retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(r -> r.getFirstName() + " " + r.getLastName())
                    .orElse(appUser.getRole() == AppUser.Role.ADMIN ? "Sistem Yöneticisi" : "Bilinmeyen Birey");
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            try {
                CompanySyncRequest company = companyServiceClient.getCompanyInfo(appUser.getIdentityNumber());
                return company.getCompanyName() != null ? company.getCompanyName() : "İsimsiz Kurum";
            } catch (Exception e) {
                log.warn("Kurumsal profil sorgusu başarısız. targetAppUserId={} errorType={}",
                        appUser.getId(), e.getClass().getSimpleName());
                return "Kurumsal Müşteri (" + appUser.getIdentityNumber() + ")";
            }
        }
        return "Bilinmeyen Kullanıcı";
    }

    private String getOwnerEmail(AppUser appUser) {
        // 🚀 DÜZELTİLDİ: Adminin e-postası da artık uydurma değil, kendi gerçek Retail profilinden geliyor!
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER || appUser.getRole() == AppUser.Role.ADMIN) {
            return retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber())
                    .map(RetailCustomer::getEmail)
                    .orElse(appUser.getRole() == AppUser.Role.ADMIN ? "admin@bank.com" : "");
        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            try {
                CompanySyncRequest company = companyServiceClient.getCompanyInfo(appUser.getIdentityNumber());
                return company.getContactEmail() != null ? company.getContactEmail() : "email_yok@kurum.com";
            } catch (Exception e) {
                return "kurumsal@sistem.com"; 
            }
        }
        return "bilinmeyen@bank.com";
    }

    private UserProfileResponse mapToUserProfileResponse(AppUser appUser) {
        RetailCustomer retail = appUser.getRole() == AppUser.Role.CORPORATE_MANAGER ? null
                : retailCustomerRepository.findByAppUser_IdentityNumber(appUser.getIdentityNumber()).orElse(null);
        return UserProfileResponse.builder()
                .identityNumber(appUser.getIdentityNumber())
                .profileName(retail == null ? getOwnerName(appUser) : (retail.getFirstName() + " " + retail.getLastName()).trim())
                .email(retail == null ? getOwnerEmail(appUser) : retail.getEmail())
                .firstName(retail == null ? null : retail.getFirstName())
                .lastName(retail == null ? null : retail.getLastName())
                .profileEditable(appUser.getRole() == AppUser.Role.CORPORATE_MANAGER || retail != null)
                .role(appUser.getRole().name())
                .status(appUser.getStatus().name())
                .accessRestrictionStatus(appUser.getAccessRestrictionStatus().name())
                .build();
    }
}
