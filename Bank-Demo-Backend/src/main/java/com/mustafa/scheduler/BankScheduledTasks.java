package com.mustafa.scheduler;

import com.mustafa.entity.Company;
import com.mustafa.repository.ICompanyRepository;
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

    @Scheduled(cron = "0 0 0 * * *") // Gece yarısı çalışır
    public void processDailyBankOperations() {
        int today = LocalDate.now().getDayOfMonth();

        // 1. Veritabanından bugünün MAAŞ işlerini topla
        List<Company> companiesToPay = companyRepository.findByAutoSalaryPaymentEnabledTrueAndSalaryPaymentDay(today);

        if (companiesToPay.isEmpty()) {
            return;
        }

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        log.info("=====================================================");
        log.info("🚀 [KARARGAH MAAŞ MOTORU] Uyandı. Saat: {}, Gün: {}", currentTime, today);

        // --- MAAŞ OPERASYONU ---
        log.info("🏢 [MAAŞ] Toplam {} şirketin maaş ödemesi başlatılıyor...", companiesToPay.size());

        for (Company company : companiesToPay) {
            try {
                companyEmployeeService.paySalariesAutomatically(company.getId(), company.getDefaultSalaryIban());
                log.info("  ✅ BAŞARILI: {} şirketinin personellerine maaşları aktarıldı.", company.getCompanyName());
            } catch (Exception e) {
                log.error("  ❌ HATA: {} şirketinin otomatik maaş ödemesi BAŞARISIZ! Sebep: {}", company.getCompanyName(), e.getMessage());
            }
        }

        log.info("🛡️ Karargah Motoru görevlerini tamamladı ve uyku moduna geçiyor...");
        log.info("=====================================================");
    }
}