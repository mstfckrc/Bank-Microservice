package com.mustafa.service.impl;

import com.mustafa.client.IAuthServiceClient;
import com.mustafa.client.IBackendServiceClient;
import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.request.*;
import com.mustafa.dto.response.*;
import com.mustafa.entity.Company;
import com.mustafa.entity.CompanyEmployee;
import com.mustafa.exception.BankOperationException;
import com.mustafa.exception.SalaryPaymentException;
import com.mustafa.security.CorporateAccess;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.ICompanyEmployeeRepository;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.service.ICompanyEmployeeService;
import feign.FeignException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyEmployeeServiceImpl implements ICompanyEmployeeService {

    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    private final ICompanyEmployeeRepository companyEmployeeRepository;
    private final ICompanyRepository companyRepository;

    // 🚀 BÜYÜK DEĞİŞİM: Eski repolar gitti, Karargah Telsizi (Feign) geldi!
    private final IBackendServiceClient backendServiceClient;
    private final RabbitMQPublisher rabbitPublisher;
    private final IAuthServiceClient authServiceClient;
    private final CorporateAccess corporateAccess;

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    @Override
    @Transactional
    public CompanyEmployeeResponse hireEmployee(String managerIdentityNumber, HireEmployeeRequest request) {
        String maskedEmployeeId = maskIdentity(request.getIdentityNumber());
        log.info("İşe alım başlatıldı. Personel Adayı: {}", maskedEmployeeId);

        Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        if (companyEmployeeRepository.existsByCompany_IdAndEmployeeIdentityNumber(company.getId(), request.getIdentityNumber())) {
            throw new BankOperationException("Bu personel zaten şirketinizde kayıtlı!");
        }

        // 📡 FEIGN 1: Müşteri Kimlik Üssünde (Auth Service) var mı? Profilini getir!
        CustomerProfileResponse profile;
        try {
            // ARTIK BACKEND'E DEĞİL, AUTH SERVİSE SORUYORUZ!
            profile = authServiceClient.getCustomerProfile(request.getIdentityNumber());
        } catch (FeignException.NotFound e) {
            log.warn("İşe alım reddedildi: Personel adayı sistemde bulunamadı.");
            throw new BankOperationException("Personel sisteme kayıtlı değil! Önce bireysel müşteri hesabı açmalıdır.");
        }

        // 📡 FEIGN 2: Verilen IBAN doğru mu ve bu adama mı ait?
        AccountValidationResponse accountValidation;
        try {
            accountValidation = backendServiceClient.validateAccount(request.getSalaryIban());
        } catch (FeignException.NotFound e) {
            throw new BankOperationException("Girilen IBAN sistemimizde bulunamadı!");
        }

        if (!accountValidation.getOwnerIdentityNumber().equals(request.getIdentityNumber())) {
            throw new BankOperationException("Güvenlik İhlali: Girilen IBAN personelin kendisine ait değil!");
        }
        if (!accountValidation.isActive()) {
            throw new BankOperationException("Girilen IBAN'a ait hesap pasif (kapatılmış) durumda!");
        }

        CompanyEmployee newEmployee = CompanyEmployee.builder()
                .company(company)
                .employeeIdentityNumber(request.getIdentityNumber())
                .firstName(profile.getFirstName()) // 🚀 Karargahtan aldığımız isimleri kendi DB'mize yazıyoruz!
                .lastName(profile.getLastName())
                .salaryAmount(request.getSalaryAmount())
                .salaryIban(request.getSalaryIban())
                .build();

        CompanyEmployee savedEmployee = companyEmployeeRepository.save(newEmployee);

        sendEmailNotification(company.getContactEmail(), "Yeni Personel İşe Alım Onayı",
                String.format("Şirketinize %s kimlik numaralı personel başarıyla kaydedilmiştir.", maskedEmployeeId), managerIdentityNumber);

        return mapToResponse(savedEmployee);
    }

    @Override
    public List<CompanyEmployeeResponse> getMyEmployees(String managerIdentityNumber) {
        Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        return companyEmployeeRepository.findByCompanyId(company.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompanyEmployeeResponse updateEmployee(String managerIdentityNumber, String employeeIdentityNumber, UpdateEmployeeRequest request) {
        Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        CompanyEmployee employeeRecord = companyEmployeeRepository
                .findByCompany_IdAndEmployeeIdentityNumber(company.getId(), employeeIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Bu TC numarasına ait bir çalışanınız bulunamadı!"));

        if (!employeeRecord.getSalaryIban().equals(request.getSalaryIban())) {
            // 📡 FEIGN 3: IBAN Değişiyorsa Karargaha Teyit Ettir
            try {
                AccountValidationResponse validation = backendServiceClient.validateAccount(request.getSalaryIban());
                if (!validation.getOwnerIdentityNumber().equals(employeeIdentityNumber)) {
                    throw new BankOperationException("Güvenlik İhlali: Yeni IBAN personelin kendisine ait değil!");
                }
                if (!validation.isActive()) throw new BankOperationException("Hesap pasif durumda!");
            } catch (FeignException.NotFound e) {
                throw new BankOperationException("Girilen yeni IBAN sistemimizde bulunamadı!");
            }
            employeeRecord.setSalaryIban(request.getSalaryIban());
        }

        employeeRecord.setSalaryAmount(request.getSalaryAmount());
        return mapToResponse(companyEmployeeRepository.save(employeeRecord));
    }

    @Override
    @Transactional
    public void removeEmployee(String managerIdentityNumber, String employeeIdentityNumber) {
        Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        CompanyEmployee employeeRecord = companyEmployeeRepository
                .findByCompany_IdAndEmployeeIdentityNumber(company.getId(), employeeIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Çalışan bulunamadı!"));

        companyEmployeeRepository.delete(employeeRecord);

        sendEmailNotification(company.getContactEmail(), "Personel Çıkış İşlemi",
                String.format("%s kimlik numaralı personelin kaydı başarıyla silinmiştir.", maskIdentity(employeeIdentityNumber)), managerIdentityNumber);
    }

    // 1️⃣ MANUEL MAAŞ DAĞITIMI
    @Override
    public List<TransactionResponse> paySalaries(String managerIdentityNumber, SalaryPaymentRequest request) {
        try {
            Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                    .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));
            validateSenderAccount(request.getSenderIban(), managerIdentityNumber);
            return processSalaryPayments(company, request.getSenderIban(), managerIdentityNumber);
        } catch (BankOperationException e) {
            throw new SalaryPaymentException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (FeignException e) {
            log.error("Maaş hesabı doğrulanamadı. httpStatus={} errorType={}", e.status(), e.getClass().getSimpleName());
            throw new SalaryPaymentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Maaş hesabı doğrulanamadı. Lütfen daha sonra tekrar deneyin.");
        }
    }

    // 2️⃣ OTOMATİK MAAŞ DAĞITIMI (Scheduler Tetikler)
    @Override
    public List<TransactionResponse> paySalariesAutomatically(Long companyId, String senderIban) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BankOperationException("Şirket bulunamadı!"));

        corporateAccess.requireCompanyAccess(company.getCompanyIdentityNumber());

        // 🚀 Ghost Login SİLİNDİ! Artık Karargahın (Monolith'in) Security duvarını aşmaya çalışmıyoruz.
        // O işi Karargahın içindeki Internal Endpoint halledecek!
        validateSenderAccount(senderIban, company.getCompanyIdentityNumber());
        return processSalaryPayments(company, senderIban, company.getCompanyIdentityNumber());
    }

    // 3️⃣ ÇEKİRDEK MOTOR (Feign ile Karargaha Emir Fırlatır)
    private List<TransactionResponse> processSalaryPayments(Company company, String senderIban, String managerId) {
        List<CompanyEmployee> employees = companyEmployeeRepository.findByCompanyId(company.getId());
        if (employees.isEmpty()) throw new BankOperationException("Şirketinize kayıtlı personel bulunmamaktadır!");

        // 🚀 BÜYÜK DEĞİŞİM: Döviz, Limit, Bakiye kontrolü GİTTİ! Sadece Devasa Bir Liste Hazırlıyoruz.
        List<SalaryPaymentItem> salaryItems = employees.stream()
                .map(emp -> new SalaryPaymentItem(emp.getSalaryIban(), emp.getSalaryAmount()))
                .collect(Collectors.toList());

        BulkSalaryRequest bulkRequest = BulkSalaryRequest.builder()
                .senderIban(senderIban)
                .companyName(company.getCompanyName())
                .companyIdentityNumber(company.getCompanyIdentityNumber())
                .salaryItems(salaryItems)
                .build();

        log.info("Karargaha (Backend) toplu maaş emri gönderiliyor. Personel Sayısı: {}", employees.size());

        List<TransactionResponse> results;
        try {
            // 📡 FEIGN 4: FÜZEYİ ATEŞLE! (Bütün yükü Karargaha devrettik)
            results = backendServiceClient.payBulkSalaries(bulkRequest);
        } catch (FeignException e) {
            log.error("Core toplu maaş çağrısı başarısız. httpStatus={} errorType={}",
                    e.status(), e.getClass().getSimpleName());
            if (e.status() == 400) {
                throw new SalaryPaymentException(HttpStatus.BAD_REQUEST, salaryRejectionMessage(e));
            }
            throw new SalaryPaymentException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Maaş işleminin sonucu doğrulanamadı. Tekrar denemeden önce hesap hareketlerini kontrol edin.");
        }

        sendEmailNotification(company.getContactEmail(), "Toplu Maaş Dağıtımı",
                "Maaş dağıtım işlemi kusursuz tamamlanmıştır.", managerId);

        return results;
    }

    private String salaryRejectionMessage(FeignException error) {
        try {
            String message = ERROR_MAPPER.readTree(error.contentUTF8()).path("message").asText();
            if (message.startsWith("Kasada yeterli bakiye yok!") || message.equals("Yetersiz bakiye!")) {
                return "Maaş dağıtımı için şirket hesabındaki bakiye yetersiz.";
            }
            return switch (message) {
                case "İşlem yapılacak hesaplardan biri kapalıdır!", "Çıkış kasası pasif durumdadır!" ->
                        "Şirket hesabı veya personelin maaş hesaplarından biri pasif. Hesap bilgilerini kontrol edin.";
                case "Alıcı hesap bulunamadı!" ->
                        "Personelin maaş hesaplarından biri bulunamadı. Personel hesap bilgilerini kontrol edin.";
                case "Aynı hesaba transfer yapamazsınız." ->
                        "Personelin maaş hesabı, seçilen şirket hesabıyla aynı olamaz.";
                case "Transfer tutarı 0'dan büyük olmalıdır!" ->
                        "Personel maaş tutarları sıfırdan büyük olmalıdır.";
                default -> "Maaş dağıtımı reddedildi. Şirket ve personel hesap bilgilerini kontrol edin.";
            };
        } catch (Exception ignored) {
            return "Maaş dağıtımı reddedildi. Şirket ve personel hesap bilgilerini kontrol edin.";
        }
    }

    @Override
    @Transactional
    public AutoPaymentSettingsResponse updateAutoPaymentSettings(String managerIdentityNumber, AutoPaymentSettingsRequest request) {
        Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        if (request.isAutoPaymentEnabled()) {
            if (request.getPaymentDay() == null || request.getPaymentDay() < 1 || request.getPaymentDay() > 31) {
                throw new BankOperationException("Geçerli bir ödeme günü (1-31) seçmelisiniz!");
            }
            validateSenderAccount(request.getDefaultSalaryIban(), managerIdentityNumber);
        }

        company.setAutoSalaryPaymentEnabled(request.isAutoPaymentEnabled());
        company.setSalaryPaymentDay(request.getPaymentDay());
        company.setDefaultSalaryIban(request.getDefaultSalaryIban());
        companyRepository.save(company);

        return AutoPaymentSettingsResponse.builder()
                .autoPaymentEnabled(company.isAutoSalaryPaymentEnabled())
                .paymentDay(company.getSalaryPaymentDay())
                .defaultSalaryIban(company.getDefaultSalaryIban())
                .message("Otomatik maaş ödeme ayarları başarıyla güncellendi.")
                .build();
    }

    @Override
    public AutoPaymentSettingsResponse getAutoPaymentSettings(String managerIdentityNumber) {
        Company company = companyRepository.findByCompanyIdentityNumber(managerIdentityNumber)
                .orElseThrow(() -> new BankOperationException("Kurumsal profil bulunamadı!"));

        return AutoPaymentSettingsResponse.builder()
                .autoPaymentEnabled(company.isAutoSalaryPaymentEnabled())
                .paymentDay(company.getSalaryPaymentDay() != null ? company.getSalaryPaymentDay() : 0)
                .defaultSalaryIban(company.getDefaultSalaryIban() != null ? company.getDefaultSalaryIban() : "")
                .message("Ayarlar getirildi.")
                .build();
    }

    // --- YARDIMCI METOTLAR ---

    private void validateSenderAccount(String iban, String expectedOwnerIdentity) {
        try {
            AccountValidationResponse validation = backendServiceClient.validateAccount(iban);
            if (!validation.getOwnerIdentityNumber().equals(expectedOwnerIdentity)) {
                throw new BankOperationException("Güvenlik İhlali: İşlem yapılmak istenen kasa size ait değil!");
            }
            if (!validation.isActive()) {
                throw new BankOperationException("Seçtiğiniz kasa pasif durumdadır!");
            }
        } catch (FeignException.NotFound e) {
            throw new BankOperationException("Kasa (IBAN) sistemde bulunamadı!");
        }
    }

    private void sendEmailNotification(String email, String subject, String content, String managerId) {
        NotificationMessage msg = NotificationMessage.builder()
                .destination(email)
                .subject(subject)
                .content(content)
                .identityNumber(maskIdentity(managerId))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();
        rabbitPublisher.sendNotification(msg);
    }

    private CompanyEmployeeResponse mapToResponse(CompanyEmployee ce) {
        return CompanyEmployeeResponse.builder()
                .id(ce.getId())
                .identityNumber(ce.getEmployeeIdentityNumber())
                .firstName(ce.getFirstName())
                .lastName(ce.getLastName())
                .salaryIban(ce.getSalaryIban())
                .salaryAmount(ce.getSalaryAmount())
                .build();
    }
}
