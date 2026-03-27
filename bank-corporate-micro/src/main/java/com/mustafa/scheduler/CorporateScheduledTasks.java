package com.mustafa.scheduler;

import com.mustafa.entity.Company;
import com.mustafa.repository.ICompanyRepository;
import com.mustafa.service.ICompanyEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 🚀 EKLENEN IMPORTLAR (Güvenlik Kalkanı İçin)
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorporateScheduledTasks {

    private final ICompanyRepository companyRepository;
    private final ICompanyEmployeeService companyEmployeeService;

    // Test etmek istersen geçici olarak bunu kullan: @Scheduled(cron = "0 * * * * *") // Her dakika başı
    @Scheduled(cron = "0 0 0 * * *")
    public void processDailySalaryOperations() {
        int today = LocalDate.now().getDayOfMonth();

        // Veritabanından bugünün MAAŞ işlerini topla
        List<Company> companiesToPay = companyRepository.findByAutoSalaryPaymentEnabledTrueAndSalaryPaymentDay(today);

        if (companiesToPay.isEmpty()) {
            return;
        }

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        log.info("=====================================================");
        log.info("🚀 [KURUMSAL MAAŞ MOTORU] Uyandı. Saat: {}, Gün: {}", currentTime, today);
        log.info("🏢 Toplam {} şirketin otomatik maaş ödemesi başlatılıyor...", companiesToPay.size());

        for (Company company : companiesToPay) {
            try {
                // 🚀 BÜYÜK MİMARİ DOKUNUŞ: Robot, o anki şirketin yöneticisinin kimliğine (TC) bürünüyor!
                // Böylece Feign Telsizi bu TC'yi alıp Karargaha yollayabilecek ve güvenlikten geçecek.
                UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken(
                        company.getCompanyIdentityNumber(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CORPORATE_MANAGER"))
                );
                SecurityContextHolder.getContext().setAuthentication(mockAuth);

                // Emri Ver!
                companyEmployeeService.paySalariesAutomatically(company.getId(), company.getDefaultSalaryIban());
                log.info("  ✅ BAŞARILI: {} şirketinin personellerine maaşları aktarıldı.", company.getCompanyName());

            } catch (Exception e) {
                log.error("  ❌ HATA: {} şirketinin otomatik maaş ödemesi BAŞARISIZ! Sebep: {}", company.getCompanyName(), e.getMessage());
            } finally {
                // 🧹 TEMİZLİK: Diğer şirketin işlemine veya hafızaya sızmasın diye kılığı (kimliği) çıkarıp atıyoruz!
                SecurityContextHolder.clearContext();
            }
        }

        log.info("🛡️ Kurumsal Motor görevlerini tamamladı ve uyku moduna geçiyor...");
        log.info("=====================================================");
    }
}