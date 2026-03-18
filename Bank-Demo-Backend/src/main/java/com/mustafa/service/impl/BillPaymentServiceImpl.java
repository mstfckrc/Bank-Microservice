package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.BillPaymentInstruction;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.IBillPaymentInstructionRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.IBillPaymentService;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.IExternalBillService; // 🚀 DÜZELTME: Artık doğru Interface'i kullanıyoruz!
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillPaymentServiceImpl implements IBillPaymentService {

    private final IBillPaymentInstructionRepository instructionRepository;
    private final IAccountRepository accountRepository;
    private final ITransactionRepository transactionRepository;
    private final ICurrencyService currencyService;
    private final IExternalBillService externalBillService; // 🚀 HATA VEREN YER DÜZELDİ

    private final RabbitMQPublisher rabbitPublisher;

    // KVKK Maskeleme Kalkanı
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    @Override
    @Transactional
    public TransactionResponse payBillAutomatically(Long instructionId) {
        BillPaymentInstruction instruction = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new BankOperationException("Talimat bulunamadı!"));

        // 1. ÇİFTE ÖDEME KONTROLÜ
        if (instruction.getLastPaymentDate() != null &&
                instruction.getLastPaymentDate().getMonth() == LocalDate.now().getMonth() &&
                instruction.getLastPaymentDate().getYear() == LocalDate.now().getYear()) {
            log.info("✅ Fatura İptali: Abone {} faturası bu ay zaten ödenmiş.", instruction.getSubscriberNo());
            return null; // Zaten ödenmişse boş döner
        }

        Account account = instruction.getAccount();
        if (!account.isActive()) {
            throw new BankOperationException("Faturaya bağlı hesap pasif durumda!");
        }

        // 2. DIŞ KURUMA BORÇ SOR (Interface üzerinden)
        BigDecimal debtInTry = externalBillService.getDebt(instruction.getSubscriberNo(), instruction.getBillType());

        // 3. DÖVİZ KONTROLÜ
        BigDecimal debtInAccountCurrency = debtInTry;
        if (!account.getCurrency().name().equalsIgnoreCase("TRY")) {
            Double converted = currencyService.convertAmount(debtInTry.doubleValue(), "TRY", account.getCurrency().name());
            debtInAccountCurrency = BigDecimal.valueOf(converted);
        }

        // 4. BAKİYE KONTROLÜ
        if (account.getBalance().compareTo(debtInAccountCurrency) < 0) {
            log.error("Fatura ödemesi başarısız! Kasa: {}, Gerekli: {} {}", account.getAccountNumber(), debtInAccountCurrency, account.getCurrency().name());
            throw new BankOperationException("Kasada yeterli bakiye yok!");
        }

        // 5. PARAYI KASADAN KES
        account.setBalance(account.getBalance().subtract(debtInAccountCurrency));
        accountRepository.save(account);

        // 6. DEKONT OLUŞTUR
        String desc = String.format("%s Faturası (Abone: %s)", instruction.getBillType().name(), instruction.getSubscriberNo());
        if (!account.getCurrency().name().equalsIgnoreCase("TRY")) {
            desc += String.format(" - Kur Çevrimi: %s TRY karşılığı", debtInTry);
        }

        Transaction transaction = Transaction.builder()
                .referenceNo(UUID.randomUUID().toString())
                .senderAccount(account)
                .amount(debtInAccountCurrency)
                .convertedAmount(debtInAccountCurrency) // Çevrilmiş tutarı da kaydedelim
                .transactionType(Transaction.TransactionType.BILL_PAYMENT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(desc)
                .build();
        transactionRepository.save(transaction);

        // 7. TALİMATI GÜNCELLE
        instruction.setLastPaymentDate(LocalDate.now());
        instructionRepository.save(instruction);

        log.info("🎉 FATURA ÖDENDİ: {} kasasından {} {} çekildi. (Abone: {})",
                account.getAccountNumber(), String.format("%.2f", debtInAccountCurrency), account.getCurrency().name(), instruction.getSubscriberNo());


        // YENİ HALİ: Fatura Ödeme DTO'su
        NotificationMessage paymentMessage = NotificationMessage.builder()
                .destination(account.getAppUser().getIdentityNumber()) // İleride müşterinin e-postası çekilebilir
                .subject("✅ Faturanız Ödendi")
                .content(String.format("%s türündeki %s numaralı aboneliğinize ait fatura (Tutar: %s %s), otomatik ödeme talimatınızla başarıyla ödenmiştir.",
                        instruction.getBillType().name(), instruction.getSubscriberNo(), debtInAccountCurrency, account.getCurrency().name()))
                .identityNumber(maskIdentity(account.getAppUser().getIdentityNumber()))
                .notificationType(NotificationMessage.NotificationType.EMAIL) // veya PUSH_NOTIFICATION
                .build();

        rabbitPublisher.sendNotification(paymentMessage);

        // 🚀 DÜZELTME: Artık düzgün bir response dönüyoruz!
        return TransactionResponse.builder()
                .referenceNo(transaction.getReferenceNo())
                .amount(transaction.getAmount())
                .convertedAmount(transaction.getConvertedAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .senderAccountId(account.getId())
                .build();
    }

    @Override
    @Transactional
    public BillInstructionResponse createInstruction(String identityNumber, BillInstructionRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BankOperationException("Kasa bulunamadı!"));

        if (!account.getAppUser().getIdentityNumber().equals(identityNumber)) {
            throw new BankOperationException("Sadece kendi hesaplarınıza talimat verebilirsiniz!");
        }

        BillPaymentInstruction instruction = BillPaymentInstruction.builder()
                .account(account)
                .billType(BillPaymentInstruction.BillType.valueOf(request.getBillType().toUpperCase()))
                .subscriberNo(request.getSubscriberNo())
                .paymentDay(request.getPaymentDay())
                .isActive(true)
                .build();

        instructionRepository.save(instruction);
        log.info("Kullanıcı ({}) yeni fatura talimatı oluşturdu. Abone: {}", maskIdentity(identityNumber), request.getSubscriberNo());

        // YENİ HALİ: Yeni Talimat DTO'su
        NotificationMessage instructionMessage = NotificationMessage.builder()
                .destination(identityNumber)
                .subject("Yeni Otomatik Ödeme Talimatı Kaydedildi")
                .content(String.format("%s türündeki %s numaralı aboneliğiniz için otomatik ödeme talimatınız başarıyla oluşturulmuştur.",
                        instruction.getBillType().name(), request.getSubscriberNo()))
                .identityNumber(maskIdentity(identityNumber))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(instructionMessage);

        return mapToResponse(instruction);
    }

    @Override
    public List<BillInstructionResponse> getMyInstructions(String identityNumber) {
        return instructionRepository.findByAccount_AppUser_IdentityNumber(identityNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteInstruction(String identityNumber, Long instructionId) {
        BillPaymentInstruction instruction = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new BankOperationException("Talimat bulunamadı!"));

        if (!instruction.getAccount().getAppUser().getIdentityNumber().equals(identityNumber)) {
            throw new BankOperationException("Yetkisiz işlem!");
        }

        instructionRepository.delete(instruction);
        log.info("Kullanıcı ({}) fatura talimatını (ID: {}) sildi.", maskIdentity(identityNumber), instructionId);

        // YENİ HALİ: İptal DTO'su
        NotificationMessage deleteMessage = NotificationMessage.builder()
                .destination(identityNumber)
                .subject("🚫 Otomatik Ödeme Talimatı İptali")
                .content(String.format("Sistemimizde kayıtlı olan %d numaralı otomatik fatura ödeme talimatınız isteğiniz üzerine iptal edilmiştir.",
                        instructionId))
                .identityNumber(maskIdentity(identityNumber))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(deleteMessage);
    }

    private BillInstructionResponse mapToResponse(BillPaymentInstruction instruction) {
        return BillInstructionResponse.builder()
                .id(instruction.getId())
                .accountNumber(instruction.getAccount().getAccountNumber())
                .iban(instruction.getAccount().getIban())
                .billType(instruction.getBillType().name())
                .subscriberNo(instruction.getSubscriberNo())
                .paymentDay(instruction.getPaymentDay())
                .lastPaymentDate(instruction.getLastPaymentDate())
                .isActive(instruction.isActive())
                .build();
    }
}