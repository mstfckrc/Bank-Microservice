package com.mustafa.scheduler;

import com.mustafa.entity.BillPaymentInstruction;
import com.mustafa.repository.IBillPaymentInstructionRepository;
import com.mustafa.service.IBillPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillScheduledTasks {

    private final IBillPaymentInstructionRepository billPaymentInstructionRepository;
    private final IBillPaymentService billPaymentService;

    @Scheduled(cron = "0 0 0 * * *")
    public void processDailyBills() {
        int today = LocalDate.now().getDayOfMonth();

        List<BillPaymentInstruction> billsToPay = billPaymentInstructionRepository.findByIsActiveTrueAndPaymentDay(today);

        if (billsToPay.isEmpty()) {
            return;
        }

        log.info("=====================================================");
        log.info("🧾 [FATURA OTOMASYONU] Toplam {} adet fatura talimatı işleniyor. Gün: {}", billsToPay.size(), today);

        for (BillPaymentInstruction instruction : billsToPay) {
            try {
                // 🚀 MİKROSERVİS GÜNCELLEMESİ: Artık identityNumber da gönderiyoruz!
                billPaymentService.payBillAutomatically(instruction.getId(), instruction.getIdentityNumber());
                log.info("Otomatik fatura ödeme çağrısı tamamlandı. instructionId={}", instruction.getId());
            } catch (Exception e) {
                log.error("Otomatik fatura ödeme çağrısı başarısız. instructionId={} errorType={}",
                        instruction.getId(), e.getClass().getSimpleName());
            }
        }

        log.info("🛡️ Fatura Motoru tüm görevlerini tamamladı ve uyku moduna geçiyor...");
        log.info("=====================================================");
    }
}
