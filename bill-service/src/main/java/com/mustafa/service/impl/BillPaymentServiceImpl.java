package com.mustafa.service.impl;

import com.mustafa.client.BackendServiceClient;
import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.request.BillInstructionRequest;
import com.mustafa.dto.request.InternalPaymentRequest;
import com.mustafa.dto.response.BillInstructionResponse;
import com.mustafa.entity.BillPaymentInstruction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IBillPaymentInstructionRepository;
import com.mustafa.service.IBillPaymentService;
import com.mustafa.service.IExternalBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillPaymentServiceImpl implements IBillPaymentService {

    private final IBillPaymentInstructionRepository instructionRepository;
    private final IExternalBillService externalBillService;
    private final BackendServiceClient backendServiceClient; // 📻 Telsizimiz
    private final RabbitMQPublisher rabbitPublisher;       // 🕊️ Posta Güvercinimiz

    // KVKK Maskeleme Kalkanı
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    @Override
    @Transactional
    public void payBillAutomatically(Long instructionId, String identityNumber) {
        BillPaymentInstruction instruction = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new BankOperationException("Talimat bulunamadı!"));

        // 1. ÇİFTE ÖDEME KONTROLÜ
        if (instruction.getLastPaymentDate() != null &&
                instruction.getLastPaymentDate().getMonth() == LocalDate.now().getMonth() &&
                instruction.getLastPaymentDate().getYear() == LocalDate.now().getYear()) {
            log.info("✅ Fatura İptali: Abone {} faturası bu ay zaten ödenmiş.", instruction.getSubscriberNo());
            return;
        }

        // 2. DIŞ KURUMA BORÇ SOR (Mock Service üzerinden)
        BigDecimal debtInTry = externalBillService.getDebt(instruction.getSubscriberNo(), instruction.getBillType());
        log.info("Fatura ödemesi başlatılıyor... Tutar: {} TRY, Hedef Kasa ID: {}", debtInTry, instruction.getAccountId());

        // 3. KARARGAHA TELSİZ AÇ (Bakiye kontrolünü ve para kesmeyi onlar yapacak!)
        InternalPaymentRequest paymentRequest = InternalPaymentRequest.builder()
                .accountId(instruction.getAccountId())
                .amount(debtInTry)
                .description(instruction.getBillType().name() + " Faturası (Abone: " + instruction.getSubscriberNo() + ")")
                .build();

        try {
            backendServiceClient.deductBillPayment(identityNumber, paymentRequest);
        } catch (Exception e) {
            log.error("Fatura ödemesi Karargah tarafından reddedildi! Sebep: {}", e.getMessage());
            throw new BankOperationException("Kasa yetersiz veya Karargah işlemi reddetti!");
        }

        // 4. BAŞARILIYSA TALİMATI GÜNCELLE
        instruction.setLastPaymentDate(LocalDate.now());
        instructionRepository.save(instruction);

        log.info("🎉 FATURA ÖDENDİ: {} ID'li kasadan {} TRY çekildi. (Abone: {})",
                instruction.getAccountId(), debtInTry, instruction.getSubscriberNo());

        // 5. MÜŞTERİYE GÜVERCİN UÇUR
        NotificationMessage paymentMessage = NotificationMessage.builder()
                .destination(identityNumber)
                .subject("✅ Faturanız Ödendi")
                .content(String.format("%s türündeki %s numaralı aboneliğinize ait fatura (Tutar: %s TRY), otomatik ödeme talimatınızla başarıyla ödenmiştir.",
                        instruction.getBillType().name(), instruction.getSubscriberNo(), debtInTry))
                .identityNumber(maskIdentity(identityNumber))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(paymentMessage);
    }

    @Override
    @Transactional
    public BillInstructionResponse createInstruction(String identityNumber, BillInstructionRequest request) {
        // MİKROSERVİS DEVRİMİ: Account'a bağlanmıyoruz, sadece kimliği yazıp geçiyoruz.
        BillPaymentInstruction instruction = BillPaymentInstruction.builder()
                .identityNumber(identityNumber)
                .accountId(request.getAccountId())
                .iban("TR******************") // Şimdilik maskeli tutuyoruz
                .billType(BillPaymentInstruction.BillType.valueOf(request.getBillType().toUpperCase()))
                .subscriberNo(request.getSubscriberNo())
                .paymentDay(request.getPaymentDay())
                .isActive(true)
                .build();

        instructionRepository.save(instruction);
        log.info("Kullanıcı ({}) yeni fatura talimatı oluşturdu. Abone: {}", maskIdentity(identityNumber), request.getSubscriberNo());

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
        return instructionRepository.findByIdentityNumber(identityNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteInstruction(String identityNumber, Long instructionId) {
        BillPaymentInstruction instruction = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new BankOperationException("Talimat bulunamadı!"));

        // GÜVENLİK: Kendi talimatı mı?
        if (!instruction.getIdentityNumber().equals(identityNumber)) {
            throw new BankOperationException("Yetkisiz işlem! Bu talimat size ait değil.");
        }

        instructionRepository.delete(instruction);
        log.info("Kullanıcı ({}) fatura talimatını (ID: {}) sildi.", maskIdentity(identityNumber), instructionId);

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
                .identityNumber(instruction.getIdentityNumber())
                .accountId(instruction.getAccountId())
                .iban(instruction.getIban())
                .billType(instruction.getBillType().name())
                .subscriberNo(instruction.getSubscriberNo())
                .paymentDay(instruction.getPaymentDay())
                .lastPaymentDate(instruction.getLastPaymentDate())
                .isActive(instruction.isActive())
                .build();
    }
}