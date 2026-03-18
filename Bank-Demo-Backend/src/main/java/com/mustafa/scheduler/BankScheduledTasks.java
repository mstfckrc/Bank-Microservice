package com.mustafa.scheduler;

import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.BillPaymentInstruction;
import com.mustafa.entity.Company;
import com.mustafa.repository.IBillPaymentInstructionRepository;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.service.IBillPaymentService;
import com.mustafa.service.ICompanyEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankScheduledTasks {

    private final ICompanyRepository companyRepository;
    private final ICompanyEmployeeService companyEmployeeService;
    private final IBillPaymentInstructionRepository billPaymentInstructionRepository;
    private final IBillPaymentService billPaymentService;

    @Scheduled(cron = "0 0 0 * * *") // Şimdilik test için dakikada bir çalışsın
    public void processDailyBankOperations() {
        int today = LocalDate.now().getDayOfMonth();

        // 1. Veritabanından bugünün işlerini topla
        List<Company> companiesToPay = companyRepository.findByAutoSalaryPaymentEnabledTrueAndSalaryPaymentDay(today);
        List<BillPaymentInstruction> billsToPay = billPaymentInstructionRepository.findByIsActiveTrueAndPaymentDay(today);

        // Eğer bugün hiçbir iş yoksa konsolu gereksiz loglarla kirletme
        if (companiesToPay.isEmpty() && billsToPay.isEmpty()) {
            return;
        }

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        log.info("=====================================================");
        log.info("🚀 [BANKA OTOMASYON MOTORU] Uyandı. Saat: {}, Gün: {}", currentTime, today);

        // --- 1. MAAŞ OPERASYONU ---
        if (!companiesToPay.isEmpty()) {
            log.info("🏢 [MAAŞ] Toplam {} şirketin maaş ödemesi başlatılıyor...", companiesToPay.size());

            for (Company company : companiesToPay) {
                try {
                    companyEmployeeService.paySalariesAutomatically(company.getId(), company.getDefaultSalaryIban());
                    log.info("  ✅ BAŞARILI: {} şirketinin personellerine maaşları aktarıldı.", company.getCompanyName());
                } catch (Exception e) {
                    log.error("  ❌ HATA: {} şirketinin otomatik maaş ödemesi BAŞARISIZ! Sebep: {}", company.getCompanyName(), e.getMessage());
                }
            }
        }

        // --- 2. FATURA OPERASYONU ---
        if (!billsToPay.isEmpty()) {
            log.info("🧾 [FATURA] Toplam {} adet fatura talimatı işleniyor...", billsToPay.size());

            for (BillPaymentInstruction instruction : billsToPay) {
                try {
                    TransactionResponse response = billPaymentService.payBillAutomatically(instruction.getId());
                    if (response != null) {
                        log.info("  ✅ BAŞARILI: Abone {} faturası ödendi. Dekont: {}", instruction.getSubscriberNo(), response.getReferenceNo());
                    }
                } catch (Exception e) {
                    log.error("  ⚠️ HATA: Fatura Talimatı (Abone: {}) Başarısız: {}", instruction.getSubscriberNo(), e.getMessage());
                }
            }
        }

        log.info("🛡️ Motor tüm görevlerini tamamladı ve uyku moduna geçiyor...");
        log.info("=====================================================");
    }
}