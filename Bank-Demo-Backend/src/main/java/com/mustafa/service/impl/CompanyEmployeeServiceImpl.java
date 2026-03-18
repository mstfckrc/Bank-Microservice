package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.*;
import com.mustafa.dto.response.AutoPaymentSettingsResponse;
import com.mustafa.dto.response.CompanyEmployeeResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.Company;
import com.mustafa.entity.CompanyEmployee;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.*;
import com.mustafa.service.ICompanyEmployeeService;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 LOGGER AKTİF
@Service
@RequiredArgsConstructor
public class CompanyEmployeeServiceImpl implements ICompanyEmployeeService {

    private final ICompanyEmployeeRepository companyEmployeeRepository;
    private final ICompanyRepository companyRepository;
    private final IRetailCustomerRepository retailCustomerRepository;
    private final IAccountRepository accountRepository;
    private final ITransactionService transactionService;
    private final ICurrencyService currencyService;

    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 KVKK Maskeleme Kalkanı
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    @Override
    @Transactional
    public CompanyEmployeeResponse hireEmployee(String managerIdentityNumber, HireEmployeeRequest request) {
        String maskedManagerId = maskIdentity(managerIdentityNumber);
        String maskedEmployeeId = maskIdentity(request.getIdentityNumber());

        log.info("Personel işe alım süreci başlatıldı. Yönetici: {}, Personel Adayı: {}", maskedManagerId, maskedEmployeeId);

        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        RetailCustomer employee = retailCustomerRepository.findByAppUser_IdentityNumber(request.getIdentityNumber())
                .orElseThrow(() -> {
                    log.warn("İşe alım reddedildi: Personel adayı ({}) sistemde kayıtlı değil.", maskedEmployeeId);
                    return new BankOperationException("Personel sisteme kayıtlı değil! Önce bireysel müşteri hesabı açmalıdır.");
                });

        boolean exists = companyEmployeeRepository.existsByCompany_IdAndRetailCustomer_AppUser_IdentityNumber(
                company.getId(), request.getIdentityNumber());
        if (exists) {
            log.warn("İşe alım reddedildi: Personel ({}) zaten bu şirkette çalışıyor.", maskedEmployeeId);
            throw new BankOperationException("Bu personel zaten şirketinizde kayıtlı!");
        }

        Account account = accountRepository.findByIban(request.getSalaryIban())
                .orElseThrow(() -> new BankOperationException("Girilen IBAN sistemimizde bulunamadı!"));

        if (!account.getAppUser().getIdentityNumber().equals(request.getIdentityNumber())) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Yönetici ({}), personel ({}) için başkasına ait bir IBAN ({}) girmeye çalıştı!",
                    maskedManagerId, maskedEmployeeId, request.getSalaryIban());
            throw new BankOperationException("Güvenlik İhlali: Girilen IBAN personelin kendisine ait değil!");
        }

        if (!account.isActive()) {
            log.warn("İşe alım reddedildi: Maaş yatırılacak hesap pasif durumda.");
            throw new BankOperationException("Girilen IBAN'a ait hesap pasif (kapatılmış) durumda!");
        }

        CompanyEmployee newEmployee = new CompanyEmployee();
        newEmployee.setCompany(company);
        newEmployee.setRetailCustomer(employee);
        newEmployee.setSalaryAmount(request.getSalaryAmount());
        newEmployee.setSalaryIban(request.getSalaryIban());

        CompanyEmployee savedEmployee = companyEmployeeRepository.save(newEmployee);
        log.info("İşe alım başarılı! Personel ({}), {} şirketine eklendi.", maskedEmployeeId, company.getCompanyName());

        // YENİ HALİ: İşe Alım DTO'su
        NotificationMessage hireMessage = NotificationMessage.builder()
                .destination(company.getContactEmail()) // Şirketin yetkili e-postasına gidiyor
                .subject("Yeni Personel İşe Alım Onayı")
                .content(String.format("Şirketinize (%s) %s kimlik numaralı personel başarıyla kaydedilmiştir.",
                        company.getCompanyName(), maskedEmployeeId))
                .identityNumber(maskedManagerId)
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(hireMessage);

        return mapToResponse(savedEmployee);
    }

    @Override
    public List<CompanyEmployeeResponse> getMyEmployees(String managerIdentityNumber) {
        log.info("Personel listesi çekiliyor. Yönetici: {}", maskIdentity(managerIdentityNumber));
        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        List<CompanyEmployee> employees = companyEmployeeRepository.findByCompanyId(company.getId());

        return employees.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompanyEmployeeResponse updateEmployee(String managerIdentityNumber, String employeeIdentityNumber, UpdateEmployeeRequest request) {
        String maskedManagerId = maskIdentity(managerIdentityNumber);
        String maskedEmployeeId = maskIdentity(employeeIdentityNumber);

        log.info("Personel güncelleme işlemi başlatıldı. Yönetici: {}, Personel: {}", maskedManagerId, maskedEmployeeId);

        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        CompanyEmployee employeeRecord = companyEmployeeRepository
                .findByCompany_IdAndRetailCustomer_AppUser_IdentityNumber(company.getId(), employeeIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Bu TC numarasına ait bir çalışanınız bulunamadı!"));

        if (!employeeRecord.getSalaryIban().equals(request.getSalaryIban())) {
            Account account = accountRepository.findByIban(request.getSalaryIban())
                    .orElseThrow(() -> new BankOperationException("Girilen yeni IBAN sistemimizde bulunamadı!"));

            if (!account.getAppUser().getIdentityNumber().equals(employeeIdentityNumber)) {
                log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Personel maaş IBAN'ı başkasına ait bir IBAN ile değiştirilmeye çalışıldı!");
                throw new BankOperationException("Güvenlik İhlali: Yeni IBAN personelin kendisine ait değil!");
            }
            if (!account.isActive()) {
                throw new BankOperationException("Girilen IBAN'a ait hesap pasif (kapatılmış) durumda!");
            }
            employeeRecord.setSalaryIban(request.getSalaryIban());
            log.info("Personel ({}) maaş IBAN bilgisi başarıyla güncellendi.", maskedEmployeeId);
        }

        employeeRecord.setSalaryAmount(request.getSalaryAmount());
        CompanyEmployee updatedEmployee = companyEmployeeRepository.save(employeeRecord);

        log.info("Personel ({}) maaş tutarı güncellendi. Yeni Tutar: {}", maskedEmployeeId, request.getSalaryAmount());

        return mapToResponse(updatedEmployee);
    }

    @Override
    @Transactional
    public void removeEmployee(String managerIdentityNumber, String employeeIdentityNumber) {
        String maskedEmployeeId = maskIdentity(employeeIdentityNumber);
        log.info("Personel işten çıkarma işlemi başlatıldı. Yönetici: {}, Personel: {}", maskIdentity(managerIdentityNumber), maskedEmployeeId);

        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        CompanyEmployee employeeRecord = companyEmployeeRepository
                .findByCompany_IdAndRetailCustomer_AppUser_IdentityNumber(company.getId(), employeeIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Bu TC numarasına ait bir çalışanınız bulunamadı!"));

        companyEmployeeRepository.delete(employeeRecord);
        log.info("Personel ({}) başarıyla şirketten çıkarıldı.", maskedEmployeeId);

        // YENİ HALİ: İşten Çıkış DTO'su
        NotificationMessage removeMessage = NotificationMessage.builder()
                .destination(company.getContactEmail())
                .subject("Personel Çıkış İşlemi")
                .content(String.format("Şirketinizden (%s), %s kimlik numaralı personelin kaydı başarıyla silinmiştir.",
                        company.getCompanyName(), maskedEmployeeId))
                .identityNumber(maskIdentity(managerIdentityNumber))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(removeMessage);
    }

    // 1️⃣ KULLANICI KAPISI (Manuel Tetikleme)
    @Override
    @Transactional
    public List<TransactionResponse> paySalaries(String managerIdentityNumber, SalaryPaymentRequest request) {
        String maskedManagerId = maskIdentity(managerIdentityNumber);
        log.info("🔥 MANUEL MAAŞ DAĞITIMI TETİKLENDİ! Yönetici: {}, Çıkış Kasası: {}", maskedManagerId, request.getSenderIban());

        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        Account senderAccount = accountRepository.findByIban(request.getSenderIban())
                .orElseThrow(() -> new BankOperationException("Ödeme yapılacak kasa bulunamadı!"));

        // 🛡️ SADECE BURADA GÜVENLİK VAR: Bu adam, bu kasanın sahibi mi?
        if (!senderAccount.getAppUser().getIdentityNumber().equals(managerIdentityNumber)) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Yönetici ({}), başkasına ait bir kasadan maaş dağıtmaya çalıştı!", maskedManagerId);
            throw new BankOperationException("Güvenlik İhlali: Sadece kendi kurumsal kasalarınızdan ödeme yapabilirsiniz!");
        }

        return processSalaryPayments(company, senderAccount);
    }

    // 2️⃣ SİSTEM KAPISI (Otomatik Zamanlayıcı İçin)
    @Override
    @Transactional
    public List<TransactionResponse> paySalariesAutomatically(Long companyId, String senderIban) {
        log.info("🤖 OTOMATİK MAAŞ DAĞITIMI TETİKLENDİ! Şirket ID: {}, Çıkış Kasası: {}", companyId, senderIban);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BankOperationException("Şirket bulunamadı!"));

        Account senderAccount = accountRepository.findByIban(senderIban)
                .orElseThrow(() -> new BankOperationException("Otomatik ödeme için belirlenen kasa bulunamadı!"));

        // 🛡️ VEKALET SİSTEMİ (GHOST LOGIN): ITransactionService'in güvenlik duvarına takılmamak için
        // işlemi şirket yöneticisi adına (vekaleten) yapıyoruz.
        String managerId = company.getAppUser().getIdentityNumber();

        try {
            // Yöneticiyi geçici olarak "Sisteme Giriş Yapmış" gibi gösteriyoruz
            UsernamePasswordAuthenticationToken ghostAuth =
                    new UsernamePasswordAuthenticationToken(managerId, null, new java.util.ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(ghostAuth);

            // Çekirdek motoru çalıştır (Artık ITransactionService yöneticiyi tanıyacak!)
            return processSalaryPayments(company, senderAccount);

        } finally {
            // 🧹 TEMİZLİK: İşlem bitince güvenlik açığı kalmaması için sahte kimliği siliyoruz.
            SecurityContextHolder.clearContext();
        }
    }

    // 3️⃣ ÇEKİRDEK MOTOR (Gizli/Private) - Kimse doğrudan erişemez, sadece işini yapar!
    private List<TransactionResponse> processSalaryPayments(Company company, Account senderAccount) {
        if (!senderAccount.isActive()) {
            throw new BankOperationException("Seçilen kasa pasif durumdadır, işlem yapılamaz!");
        }

        List<CompanyEmployee> employees = companyEmployeeRepository.findByCompanyId(company.getId());
        if (employees.isEmpty()) {
            log.warn("Maaş Dağıtımı İptali: Şirketin ({}) hiç kayıtlı personeli yok.", company.getCompanyName());
            throw new BankOperationException("Şirketinize kayıtlı personel bulunmamaktadır!");
        }

        BigDecimal totalSalaryInTry = employees.stream()
                .map(CompanyEmployee::getSalaryAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRequiredInSenderCurrency = totalSalaryInTry;

        // DÖVİZ KONTROLÜ
        if (!senderAccount.getCurrency().name().equalsIgnoreCase("TRY")) {
            Double convertedTotal = currencyService.convertAmount(
                    totalSalaryInTry.doubleValue(), "TRY", senderAccount.getCurrency().name()
            );
            totalRequiredInSenderCurrency = BigDecimal.valueOf(convertedTotal);
            log.info("Döviz Koruması: Toplam {} TRY maaş yükü, anlık kurla {} {} olarak hesaplandı.",
                    totalSalaryInTry, String.format("%.2f", totalRequiredInSenderCurrency), senderAccount.getCurrency().name());
        }

        // BAKİYE KONTROLÜ
        if (senderAccount.getBalance().compareTo(totalRequiredInSenderCurrency) < 0) {
            log.error("Maaş Dağıtımı Başarısız: Bakiye Yetersiz! Gerekli: {} {}, Mevcut: {} {}",
                    String.format("%.2f", totalRequiredInSenderCurrency), senderAccount.getCurrency().name(),
                    senderAccount.getBalance(), senderAccount.getCurrency().name());

            throw new BankOperationException("Kasada yeterli bakiye yok! Toplam Gereken: " +
                    String.format("%.2f", totalRequiredInSenderCurrency) + " " + senderAccount.getCurrency().name());
        }

        log.info("Bakiye onayı alındı. {} personel için transfer döngüsü başlıyor...", employees.size());

        List<TransactionResponse> transactionResults = new ArrayList<>();

        for (CompanyEmployee emp : employees) {
            BigDecimal amountInSenderCurrency = emp.getSalaryAmount();

            if (!senderAccount.getCurrency().name().equalsIgnoreCase("TRY")) {
                Double converted = currencyService.convertAmount(
                        emp.getSalaryAmount().doubleValue(), "TRY", senderAccount.getCurrency().name()
                );
                amountInSenderCurrency = BigDecimal.valueOf(converted);
            }

            // TRANSFER İŞLEMİ (MASAK'a da takılırsa ITransactionService halleder)
            com.mustafa.dto.request.TransferRequest transferReq = new com.mustafa.dto.request.TransferRequest();
            transferReq.setSenderIban(senderAccount.getIban());
            transferReq.setReceiverIban(emp.getSalaryIban());
            transferReq.setAmount(amountInSenderCurrency);
            transferReq.setDescription("Maaş Ödemesi - " + company.getCompanyName());
            transferReq.setSalaryPayment(true);

            TransactionResponse response = transactionService.transfer(transferReq);
            transactionResults.add(response);
        }

        log.info("✅ Toplu maaş dağıtımı KUSURSUZ tamamlandı. Toplam Aktarılan: {} TRY", totalSalaryInTry);

        // YENİ HALİ: Toplu Maaş Raporu DTO'su
        NotificationMessage salaryReportMessage = NotificationMessage.builder()
                .destination(company.getContactEmail())
                .subject("✅ Toplu Maaş Dağıtımı Tamamlandı")
                .content(String.format("%s şirketinizin %d personeli için toplam %s TRY maaş dağıtım işlemi kusursuz bir şekilde tamamlanmıştır.",
                        company.getCompanyName(), employees.size(), totalSalaryInTry))
                .identityNumber(maskIdentity(company.getAppUser().getIdentityNumber()))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(salaryReportMessage);

        return transactionResults;
    }

    @Override
    @Transactional
    public AutoPaymentSettingsResponse updateAutoPaymentSettings(String managerIdentityNumber, AutoPaymentSettingsRequest request) {
        log.info("Otomatik maaş ödeme ayarları güncelleniyor. Yönetici: {}", maskIdentity(managerIdentityNumber));

        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        if (request.isAutoPaymentEnabled()) {
            if (request.getPaymentDay() == null || request.getPaymentDay() < 1 || request.getPaymentDay() > 31) {
                log.warn("Otomatik ödeme ayarı hatası: Geçersiz gün ({}).", request.getPaymentDay());
                throw new BankOperationException("Geçerli bir ödeme günü (1-31 arası) seçmelisiniz!");
            }
            if (request.getDefaultSalaryIban() == null || request.getDefaultSalaryIban().isBlank()) {
                log.warn("Otomatik ödeme ayarı hatası: IBAN boş.");
                throw new BankOperationException("Otomatik ödeme için geçerli bir kasa IBAN'ı seçmelisiniz!");
            }

            Account account = accountRepository.findByIban(request.getDefaultSalaryIban())
                    .orElseThrow(() -> new BankOperationException("Seçilen kasa bulunamadı!"));

            if (!account.getAppUser().getIdentityNumber().equals(managerIdentityNumber)) {
                log.warn("🚨 GÜVENLİK İHLALİ! Şirket ({}) otomatik ödeme için başkasının IBAN'ını kaydetmeye çalıştı: {}",
                        company.getCompanyName(), request.getDefaultSalaryIban());
                throw new BankOperationException("Sadece kendi şirketinize ait kasaları otomatik ödeme için seçebilirsiniz!");
            }
            if (!account.isActive()) {
                throw new BankOperationException("Seçtiğiniz kasa pasif durumdadır. Otomatik ödeme için aktif bir kasa seçiniz!");
            }
        }

        // Kontrollerden geçtiyse veritabanına yaz
        company.setAutoSalaryPaymentEnabled(request.isAutoPaymentEnabled());
        company.setSalaryPaymentDay(request.getPaymentDay());
        company.setDefaultSalaryIban(request.getDefaultSalaryIban());

        companyRepository.save(company);

        log.info("✅ Şirket ({}) otomatik maaş ayarları kaydedildi. Aktif mi: {}, Gün: {}, Kasa: {}",
                company.getCompanyName(), request.isAutoPaymentEnabled(), request.getPaymentDay(), request.getDefaultSalaryIban());

        // YENİ HALİ: Ayar Güncelleme DTO'su
        NotificationMessage settingsMessage = NotificationMessage.builder()
                .destination(company.getContactEmail())
                .subject("Otomatik Maaş Ayarları Güncellendi")
                .content(String.format("%s şirketinizin otomatik maaş ödeme ayarları güncellenmiştir. Aktiflik Durumu: %b",
                        company.getCompanyName(), request.isAutoPaymentEnabled()))
                .identityNumber(maskIdentity(managerIdentityNumber))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(settingsMessage);

        return AutoPaymentSettingsResponse.builder()
                .autoPaymentEnabled(company.isAutoSalaryPaymentEnabled())
                .paymentDay(company.getSalaryPaymentDay())
                .defaultSalaryIban(company.getDefaultSalaryIban())
                .message("Otomatik maaş ödeme ayarları başarıyla güncellendi.")
                .build();
    }

    @Override
    public AutoPaymentSettingsResponse getAutoPaymentSettings(String managerIdentityNumber) {
        Company company = companyRepository.findByAppUser_IdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        return AutoPaymentSettingsResponse.builder()
                .autoPaymentEnabled(company.isAutoSalaryPaymentEnabled())
                .paymentDay(company.getSalaryPaymentDay() != null ? company.getSalaryPaymentDay() : 0)
                .defaultSalaryIban(company.getDefaultSalaryIban() != null ? company.getDefaultSalaryIban() : "")
                .message("Mevcut ayarlar başarıyla getirildi.")
                .build();
    }

    private CompanyEmployeeResponse mapToResponse(CompanyEmployee ce) {
        return CompanyEmployeeResponse.builder()
                .id(ce.getId())
                .identityNumber(ce.getRetailCustomer().getAppUser().getIdentityNumber())
                .firstName(ce.getRetailCustomer().getFirstName())
                .lastName(ce.getRetailCustomer().getLastName())
                .salaryIban(ce.getSalaryIban())
                .salaryAmount(ce.getSalaryAmount())
                .build();
    }
}