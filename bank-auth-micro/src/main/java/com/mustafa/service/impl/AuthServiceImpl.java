package com.mustafa.service.impl;

import com.mustafa.client.CompanyServiceClient;
import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.dto.request.RegisterRequest;
import com.mustafa.dto.response.AuthResponse;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.IAuthService;
import com.mustafa.service.ICaptchaService; // 🚀 Siber Kalkan Arayüzü

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IAppUserRepository appUserRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final RabbitMQPublisher rabbitPublisher;
    private final CompanyServiceClient companyServiceClient;
    private final Keycloak keycloak;

    // 🚀 SİBER KALKAN (Interface üzerinden güvenli bağlantı)
    private final ICaptchaService captchaService;

    @Value("${keycloak.realm}")
    private String realm;

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String maskedId = maskIdentity(request.getIdentityNumber());
        log.info("Kayıt işlemi başlatıldı. Kimlik/Vergi No: {}, Rol: {}", maskedId, request.getRole());

        // 🚀 0. ADIM: SİBER KALKAN KONTROLÜ (Veritabanına gitmeden önce fail-fast)
        boolean isHuman = captchaService.verifyToken(request.getCaptchaToken());
        if (!isHuman) {
            log.warn("Saldırı Engellendi: {} kimlik numaralı kayıt isteği Google Captcha testini geçemedi!", maskedId);
            throw new BankOperationException("Güvenlik doğrulaması başarısız! Lütfen robot olmadığınızı kanıtlayıp tekrar deneyin.");
        }

        // 1. KONTROL (Kendi veritabanımızda var mı?)
        if (appUserRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            log.warn("Kayıt Reddedildi: {} kimlik numarası sistemde zaten mevcut!", maskedId);
            throw new BankOperationException("Bu Kimlik/Vergi Numarası sistemde zaten kayıtlı!");
        }

        // 2. KEYCLOAK KAYDI: Önce Nüfus Müdürlüğüne (Keycloak) adamı kaydet ve UUID'sini al!
        String keycloakId = createKeycloakUser(request);

        // 3. MERKEZİ KİMLİK: (AppUser) Kendi Veritabanımızda Oluştur
        AppUser appUser = AppUser.builder()
                .identityNumber(request.getIdentityNumber())
                .keycloakId(keycloakId) // Şifre değil, Keycloak ID tutuyoruz
                .role(AppUser.Role.valueOf(request.getRole()))
                .status(AppUser.ApprovalStatus.PENDING)
                .build();
        appUserRepository.save(appUser);
        log.info("Merkezi kullanıcı (AppUser) başarıyla oluşturuldu. ID: {}", appUser.getId());

        // 4. BİREYSEL / KURUMSAL AYRIMI
        if (appUser.getRole() == AppUser.Role.RETAIL_CUSTOMER) {
            if (retailCustomerRepository.existsByEmail(request.getEmail())) {
                log.warn("Kayıt Reddedildi: {} e-posta adresi bireysel hesaplar için kullanımda!", request.getEmail());
                throw new BankOperationException("Bu Email adresi zaten kullanımda!");
            }
            RetailCustomer retailCustomer = RetailCustomer.builder()
                    .appUser(appUser)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .build();
            retailCustomerRepository.save(retailCustomer);
            log.info("Bireysel Müşteri (RetailCustomer) profili oluşturuldu.");

        } else if (appUser.getRole() == AppUser.Role.CORPORATE_MANAGER) {
            log.info("Kurumsal Yönetici kimliği oluşturuldu. Şirket adı: {}. Kurumsal Servise telsiz atılıyor...", request.getCompanyName());
            try {
                CompanySyncRequest syncRequest = CompanySyncRequest.builder()
                        .companyIdentityNumber(request.getIdentityNumber())
                        .companyName(request.getCompanyName())
                        .contactEmail(request.getEmail())
                        .taxOffice(request.getTaxOffice())
                        .build();

                companyServiceClient.syncNewCompany(syncRequest);
                log.info("✅ Kurumsal Servis ile senkronizasyon başarılı!");
            } catch (Exception e) {
                log.error("❌ Kurumsal Servise telsiz atılamadı! Sebep: {}", e.getMessage());
                throw new BankOperationException("Kurumsal servis ile bağlantı kurulamadı, kayıt işlemi iptal edildi!");
            }
        } else {
            log.error("Kayıt sırasında geçersiz rol tespiti: {}", request.getRole());
            throw new BankOperationException("Geçersiz veya yetkisiz rol seçimi!");
        }

        // 5. KARŞILAMA MESAJI (RabbitMQ)
        NotificationMessage welcomeMessage = NotificationMessage.builder()
                .destination(request.getEmail())
                .subject("Bankamıza Hoş Geldiniz")
                .content(String.format("Sayın kullanıcımız, %s rolü ile sisteme kayıt başvurunuz alınmıştır. Hesabınız yönetici tarafından onaylandığında işlemlerinize başlayabilirsiniz.", request.getRole()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();
        rabbitPublisher.sendNotification(welcomeMessage);

        // 6. SONUÇ: Başarılı mesajı dön (Token artık Keycloak'tan alınacak)
        return AuthResponse.builder()
                .token("")
                .message("Kayıt işlemi başarıyla gerçekleşti. Güvenlik ekranından giriş yapabilirsiniz.")
                .build();
    }

    // 🚀 KEYCLOAK YARDIMCI METODU
    private String createKeycloakUser(RegisterRequest request) {

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getIdentityNumber());
        user.setEmail(request.getEmail());

        // 🚀 TAKTİK 1: Kurumsal şirketleri Keycloak'ta isimsiz bırakmıyoruz (Kapıdaki Formu Engelleme)
        if (AppUser.Role.CORPORATE_MANAGER.name().equals(request.getRole())) {
            user.setFirstName(request.getCompanyName() != null ? request.getCompanyName() : "Bilinmeyen Şirket");
            user.setLastName("Kurumsal");
        } else {
            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName() != null) user.setLastName(request.getLastName());
        }

        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        user.setCredentials(Collections.singletonList(credential));

        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() != 201) {
            log.error("Keycloak Kullanıcı Kaydı Başarısız! Status Code: {}", response.getStatus());
            throw new BankOperationException("Güvenlik sunucusunda hesap oluşturulamadı! Status: " + response.getStatus());
        }

        String path = response.getLocation().getPath();
        String keycloakUserId = path.substring(path.lastIndexOf('/') + 1);

        log.info("Keycloak kaydı başarılı. Üretilen Keycloak ID: {}", keycloakUserId);

        // 🚀 DÜZELTME: Middleware'in (Frontend) doğru yönlendirme yapabilmesi için Rol/Rütbe Ataması!
        try {
            RoleRepresentation realmRole =
                    keycloak.realm(realm).roles().get(request.getRole()).toRepresentation();
            
            keycloak.realm(realm).users().get(keycloakUserId).roles().realmLevel()
                    .add(Collections.singletonList(realmRole));
            
            log.info("Keycloak rol ataması başarılı: {}", request.getRole());
        } catch (Exception e) {
            log.error("Keycloak rol ataması sırasında hata! Rol: {}, Hata: {}", request.getRole(), e.getMessage());
            throw new BankOperationException("Kullanıcı oluşturuldu ancak yetki (rol) atanamadı!");
        }

        return keycloakUserId;
    }
}