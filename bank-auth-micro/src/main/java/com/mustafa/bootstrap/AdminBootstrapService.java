package com.mustafa.bootstrap;

import com.mustafa.entity.BootstrapRecord;
import com.mustafa.validation.IdentityNumberValidator;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** One-time setup of a new demo administrator; never an elevation endpoint. */
@Service
public class AdminBootstrapService {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);
    private static final String COMPLETION = "initial-bank-admin-v1";
    private static final String IDENTITY_ATTRIBUTE = "bank_identity_number";
    private static final String BOOTSTRAP_ATTRIBUTE = "bank_bootstrap_id";
    private static final Set<String> BANK_ROLES = Set.of("ADMIN", "RETAIL_CUSTOMER", "CORPORATE_MANAGER");
    private static final int REALM_READY_ATTEMPTS = 24;
    private static final long REALM_READY_RETRY_MILLIS = 5_000L;

    private final Keycloak keycloak;
    private final AdminBootstrapStateService stateService;

    @Value("${keycloak.realm}") private String realmName;
    @Value("${bank.bootstrap.admin.identity-number:}") private String identity;
    @Value("${bank.bootstrap.admin.first-name:}") private String firstName;
    @Value("${bank.bootstrap.admin.last-name:}") private String lastName;
    @Value("${bank.bootstrap.admin.email:}") private String email;
    @Value("${bank.bootstrap.admin.password:}") private String password;
    @Value("${bank.bootstrap.admin.marker:}") private String marker;

    public AdminBootstrapService(Keycloak keycloak, AdminBootstrapStateService stateService) {
        this.keycloak = keycloak;
        this.stateService = stateService;
    }

    public void prepareInitialAdmin() {
        BootstrapRecord.State state = stateService.state();
        if (state == BootstrapRecord.State.COMPLETE) {
            return;
        }
        validateInputs();
        if (state == null) {
            stateService.assertCanStart();
        }
        awaitRealmReady();
        String expectedKeycloakId = state == BootstrapRecord.State.PREPARED
                ? stateService.preparedKeycloakId(identity) : null;
        String keycloakId = prepareKeycloakUser(expectedKeycloakId);
        stateService.prepare(identity, keycloakId);
        ensureKeycloakAdminEnabled(keycloakId);
        stateService.complete(identity, keycloakId);
        log.info("İlk banka yöneticisi kurulumu tamamlandı.");
    }

    private void awaitRealmReady() {
        for (int attempt = 1; attempt <= REALM_READY_ATTEMPTS; attempt++) {
            try {
                keycloak.realm(realmName).toRepresentation();
                log.info("Keycloak realm hazır; ilk yönetici kurulumu sürdürülecek.");
                return;
            } catch (ProcessingException error) {
                log.warn("Keycloak bağlantısı henüz hazır değil; deneme {}/{}.",
                        attempt, REALM_READY_ATTEMPTS);
            } catch (WebApplicationException error) {
                int status = error.getResponse() == null ? 0 : error.getResponse().getStatus();
                if (status != 404 && status != 502 && status != 503 && status != 504) {
                    throw new IllegalStateException(
                            "Keycloak readiness kontrolü reddedildi; HTTP " + status + ".");
                }
                log.warn("Keycloak realm henüz hazır değil; HTTP {}, deneme {}/{}.",
                        status, attempt, REALM_READY_ATTEMPTS);
            }

            if (attempt < REALM_READY_ATTEMPTS) {
                try {
                    Thread.sleep(REALM_READY_RETRY_MILLIS);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Keycloak hazır olma bekleyişi kesildi.");
                }
            }
        }
        throw new IllegalStateException("Keycloak realm 24 denemede hazır olmadı; ilk yönetici kurulumu tamamlanmadı.");
    }

    private void validateInputs() {
        if (!IdentityNumberValidator.isValidTckn(identity)) {
            throw new IllegalStateException("İlk yönetici için geçerli TCKN biçimi gerekli.");
        }
        if (!validText(firstName) || !validText(lastName) || !validText(email)
                || password == null || password.length() < 12
                || marker == null || !marker.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("İlk yönetici adı, soyadı, e-posta ve kurulum secret'larını kontrol edin.");
        }
    }

    private boolean validText(String value) {
        return value != null && !value.isBlank() && value.length() <= 100 && value.equals(value.trim());
    }

    private String prepareKeycloakUser(String expectedKeycloakId) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            String id = expectedKeycloakId;
            if (id == null) {
                List<UserRepresentation> matches = realm.users().searchByUsername(identity, true);
                if (matches.size() > 1) {
                    throw new IllegalStateException("İlk yönetici kimliği benzersiz değil; kurulum durduruldu.");
                }
                id = matches.isEmpty() ? createKeycloakUser(realm) : matches.get(0).getId();
            }
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("İlk yönetici kimlik sunucusunda doğrulanamadı.");
            }
            UserResource resource = realm.users().get(id);
            UserRepresentation user = resource.toRepresentation();
            if (!identity.equals(user.getUsername()) || !firstName.equals(user.getFirstName())
                    || !lastName.equals(user.getLastName())
                    || !email.equals(user.getEmail())
                    || !hasSingleAttribute(user, IDENTITY_ATTRIBUTE, identity)
                    || !hasSingleAttribute(user, BOOTSTRAP_ATTRIBUTE, marker)) {
                throw new IllegalStateException("Mevcut Keycloak hesabı ilk kurulumla eşleşmiyor; yetki verilmedi.");
            }
            rejectConflictingBankRoles(resource);
            return id;
        } catch (WebApplicationException error) {
            // Keep the status useful for diagnosis; never expose remote response bodies.
            int status = error.getResponse() == null ? 0 : error.getResponse().getStatus();
            throw new IllegalStateException(
                    "İlk yönetici için Keycloak isteği başarısız; HTTP " + status + ".");
        } catch (ProcessingException error) {
            throw new IllegalStateException(
                    "İlk yönetici için Keycloak bağlantısı başarısız; kurulum tamamlanmadı.");
        }
    }

    private String createKeycloakUser(RealmResource realm) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(identity);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        // Keep a partially created identity unable to sign in before its local
        // AppUser checkpoint has committed.
        user.setEnabled(false);
        user.setAttributes(Map.of(IDENTITY_ATTRIBUTE, List.of(identity), BOOTSTRAP_ATTRIBUTE, List.of(marker)));
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));
        try (Response response = realm.users().create(user)) {
            if (response.getStatus() != 201 || response.getLocation() == null) {
                throw new IllegalStateException(
                        "İlk yönetici Keycloak'ta oluşturulamadı; HTTP " + response.getStatus() + ".");
            }
            String path = response.getLocation().getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    private void ensureKeycloakAdminEnabled(String id) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            UserResource resource = realm.users().get(id);
            UserRepresentation user = resource.toRepresentation();
            if (!identity.equals(user.getUsername()) || !firstName.equals(user.getFirstName())
                    || !lastName.equals(user.getLastName()) || !email.equals(user.getEmail())
                    || !hasSingleAttribute(user, IDENTITY_ATTRIBUTE, identity)
                    || !hasSingleAttribute(user, BOOTSTRAP_ATTRIBUTE, marker)) {
                throw new IllegalStateException("Bekleyen Keycloak hesabı ilk kurulumla eşleşmiyor; yetki verilmedi.");
            }
            List<String> bankRoles = bankRoles(resource);
            if (bankRoles.stream().anyMatch(role -> !"ADMIN".equals(role))) {
                throw new IllegalStateException("İlk yönetici hesabında çelişkili banka rolü var.");
            }
            if (!bankRoles.contains("ADMIN")) {
                RoleRepresentation adminRole = realm.roles().get("ADMIN").toRepresentation();
                resource.roles().realmLevel().add(List.of(adminRole));
            }
            if (!Boolean.TRUE.equals(user.isEnabled())) {
                user.setEnabled(true);
                resource.update(user);
            }
        } catch (WebApplicationException error) {
            // Keep the status useful for diagnosis; never expose remote response bodies.
            int status = error.getResponse() == null ? 0 : error.getResponse().getStatus();
            throw new IllegalStateException(
                    "İlk yönetici yetkilendirmesi Keycloak'ta tamamlanamadı; HTTP " + status + ".");
        } catch (ProcessingException error) {
            throw new IllegalStateException(
                    "İlk yönetici yetkilendirmesinde Keycloak bağlantısı başarısız.");
        }
    }

    private void rejectConflictingBankRoles(UserResource resource) {
        if (bankRoles(resource).stream().anyMatch(role -> !"ADMIN".equals(role))) {
            throw new IllegalStateException("İlk yönetici hesabında çelişkili banka rolü var; yerel kayıt oluşturulmadı.");
        }
    }

    private List<String> bankRoles(UserResource resource) {
        return resource.roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName).filter(BANK_ROLES::contains).toList();
    }

    private boolean hasSingleAttribute(UserRepresentation user, String name, String expected) {
        return user.getAttributes() != null && List.of(expected).equals(user.getAttributes().get(name));
    }
}
