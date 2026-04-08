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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
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

    // 🚀 YENİ: Keycloak Telsizi
    private final Keycloak keycloak;

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

        // 1. KONTROL (Kendi veritabanımızda var mı?)
        if (appUserRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            log.warn("Kayıt Reddedildi: {} kimlik numarası sistemde zaten mevcut!", maskedId);
            throw new BankOperationException("Bu Kimlik/Vergi Numarası sistemde zaten kayıtlı!");
        }

        // 🚀 2. BÜYÜK DEĞİŞİM: Önce Keycloak'a adamı kaydet ve UUID'sini al!
        String keycloakId = createKeycloakUser(request);

        // 3. Merkezi Kimliği (AppUser) Kendi Veritabanımızda Oluştur
        AppUser appUser = AppUser.builder()
                .identityNumber(request.getIdentityNumber())
                .keycloakId(keycloakId) // ARTIK ŞİFRE DEĞİL, KEYCLOAK ID TUTUYORUZ!
                .role(AppUser.Role.valueOf(request.getRole()))
                .status(AppUser.ApprovalStatus.PENDING)
                .build();
        appUserRepository.save(appUser);
        log.info("Merkezi kullanıcı (AppUser) başarıyla oluşturuldu. ID: {}", appUser.getId());

        // 4. Bireysel / Kurumsal Fabrika Mantığı (Aynen Kalıyor)
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

        // 5. Karşılama Mesajını Fırlat
        NotificationMessage welcomeMessage = NotificationMessage.builder()
                .destination(request.getEmail())
                .subject("Bankamıza Hoş Geldiniz")
                .content(String.format("Sayın kullanıcımız, %s rolü ile sisteme kayıt başvurunuz alınmıştır. Hesabınız yönetici tarafından onaylandığında işlemlerinize başlayabilirsiniz.", request.getRole()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();
        rabbitPublisher.sendNotification(welcomeMessage);

        // 🚀 6. SONUÇ: Artık token üretmiyoruz! Frontend'e sadece başarılı mesajı dönüyoruz.
        return AuthResponse.builder()
                .token("") // Token artık yok, boş bırakıyoruz. Müşteri gidip Keycloak'tan giriş yapacak.
                .message("Kayıt işlemi başarıyla gerçekleşti. Güvenlik ekranından giriş yapabilirsiniz.")
                .build();
    }

    // 🚀 KEYCLOAK YARDIMCI METODU (İmportları temizlenmiş hali)
    private String createKeycloakUser(RegisterRequest request) {

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getIdentityNumber());
        user.setEmail(request.getEmail());

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
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
        return keycloakUserId;
    }
}