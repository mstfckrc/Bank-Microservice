package com.mustafa.listener;

import com.mustafa.config.RabbitMQConfig;
import com.mustafa.dto.message.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

    /**
     * 🚀 NÖBETÇİ ARTIK DTO (JSON) OKUYOR!
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receiveNotification(NotificationMessage notification) {

        log.info("---------------------------------------------------");
        log.info("📩 [POSTANE NÖBETÇİSİ UYANDI] Yeni bir JSON Zarfı alındı!");
        log.info("📌 TİP     : {}", notification.getNotificationType());
        log.info("🎯 HEDEF   : {}", notification.getDestination());
        log.info("🏷️ BAŞLIK  : {}", notification.getSubject());
        log.info("✉️ İÇERİK  : {}", notification.getContent());

        // --- AĞIR İŞÇİLİK BURADA YAPILIR ---
        try {
            switch (notification.getNotificationType()) {
                case SMS:
                    log.info("📱 Müşterinin telefonuna SMS gönderiliyor...");
                    break;
                case EMAIL:
                    log.info("📧 Müşterinin adresine E-Posta gönderiliyor...");
                    break;
                case PUSH_NOTIFICATION:
                    log.info("🔔 Müşterinin mobil cihazına Bildirim (Push) fırlatılıyor...");
                    break;
                case SYSTEM_ALERT:
                    log.info("🚨 DİKKAT: Sistem Alarmı paneline kritik uyarı düşürülüyor!");
                    break;
                default:
                    log.warn("Bilinmeyen bildirim tipi alındı!");
            }

            Thread.sleep(2000); // Gönderim simülasyonu

        } catch (InterruptedException e) {
            log.error("Nöbetçi asker uyandırılırken hata oluştu!", e);
        }

        log.info("✅ GÖREV TAMAM: Bildirim iletildi. Zarf imha edildi!");
        log.info("---------------------------------------------------\n");
    }
}