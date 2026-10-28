package com.mustafa.bootstrap;

import com.mustafa.entity.AppUser;
import com.mustafa.entity.BootstrapRecord;
import com.mustafa.repository.IBootstrapRecordRepository;
import com.mustafa.repository.IAppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Commits local bootstrap checkpoints separately from remote Keycloak operations. */
@Service
public class AdminBootstrapStateService {
    private static final String COMPLETION = "initial-bank-admin-v1";

    private final IAppUserRepository users;
    private final IBootstrapRecordRepository records;

    public AdminBootstrapStateService(IAppUserRepository users, IBootstrapRecordRepository records) {
        this.users = users;
        this.records = records;
    }

    @Transactional(readOnly = true)
    public BootstrapRecord.State state() {
        return records.findById(COMPLETION).map(BootstrapRecord::getState).orElse(null);
    }

    @Transactional(readOnly = true)
    public void assertCanStart() {
        if (!records.existsById(COMPLETION) && users.count() != 0) {
            throw new IllegalStateException("İlk yönetici kurulumu yalnız boş Auth veritabanında başlatılabilir.");
        }
    }

    @Transactional(readOnly = true)
    public String preparedKeycloakId(String identity) {
        BootstrapRecord record = records.findById(COMPLETION)
                .orElseThrow(() -> new IllegalStateException("Bekleyen ilk yönetici kurulum kaydı bulunamadı."));
        if (record.getState() != BootstrapRecord.State.PREPARED) {
            throw new IllegalStateException("İlk yönetici kurulum durumu beklenen aşamada değil.");
        }
        AppUser admin = users.findByIdentityNumber(identity)
                .orElseThrow(() -> new IllegalStateException("Bekleyen ilk yönetici yerel kaydı bulunamadı."));
        if (admin.getRole() != AppUser.Role.ADMIN || admin.getKeycloakId() == null
                || admin.getKeycloakId().isBlank()) {
            throw new IllegalStateException("Bekleyen ilk yönetici kimlik eşlemesi eksik.");
        }
        return admin.getKeycloakId();
    }

    @Transactional
    public void prepare(String identity, String keycloakId) {
        BootstrapRecord existing = records.findById(COMPLETION).orElse(null);
        if (existing != null) {
            verifyPreparedAdmin(existing, identity, keycloakId);
            return;
        }
        if (users.count() != 0) {
            throw new IllegalStateException("İlk yönetici kurulumu yalnız boş Auth veritabanında başlatılabilir.");
        }

        AppUser admin = AppUser.builder()
                .identityNumber(identity)
                .keycloakId(keycloakId)
                .role(AppUser.Role.ADMIN)
                .status(AppUser.ApprovalStatus.APPROVED)
                .accessRestrictionStatus(AppUser.AccessRestrictionStatus.UNRESTRICTED)
                .build();
        users.saveAndFlush(admin);
        records.saveAndFlush(new BootstrapRecord(COMPLETION, BootstrapRecord.State.PREPARED));
    }

    @Transactional
    public void complete(String identity, String keycloakId) {
        BootstrapRecord record = records.findById(COMPLETION)
                .orElseThrow(() -> new IllegalStateException("İlk yönetici kurulum kaydı bulunamadı."));
        verifyPreparedAdmin(record, identity, keycloakId);
        record.complete();
    }

    private void verifyPreparedAdmin(BootstrapRecord record, String identity, String keycloakId) {
        if (record.getState() == null) {
            throw new IllegalStateException("İlk yönetici kurulum kaydı geçersiz; kurulum durduruldu.");
        }
        if (record.getState() == BootstrapRecord.State.COMPLETE) {
            return;
        }
        AppUser admin = users.findByIdentityNumber(identity)
                .orElseThrow(() -> new IllegalStateException("Bekleyen ilk yönetici yerel kaydı bulunamadı."));
        if (admin.getRole() != AppUser.Role.ADMIN || !keycloakId.equals(admin.getKeycloakId())
                || admin.getStatus() != AppUser.ApprovalStatus.APPROVED
                || admin.getAccessRestrictionStatus() != AppUser.AccessRestrictionStatus.UNRESTRICTED) {
            throw new IllegalStateException("Bekleyen ilk yönetici kaydı kurulum girdileriyle eşleşmiyor.");
        }
    }
}
