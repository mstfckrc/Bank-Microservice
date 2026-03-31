package com.mustafa.dto.message; // Kendi paket yapına göre ayarla

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    // 1. Kime Gidecek? (E-posta adresi, telefon numarası veya sistem yöneticisi emaili)
    private String destination;

    // 2. Mesajın Konusu (Mail başlığı veya SMS başlığı)
    private String subject;

    // 3. Asıl Mesaj Metni (Eskiden String olarak attığımız o uzun mesajlar buraya gelecek)
    private String content;

    // 4. Loglarda veya hata takibinde (Trace) kullanmak için müşterinin maskeli kimliği
    private String identityNumber;

    // 5. Bildirimin Tipi (Sistem bu tipe bakıp Mail mi, SMS mi atacağına karar verecek)
    private NotificationType notificationType;

    // 🚀 KURUMSAL STANDART: Enum kullanarak hatalı tip girilmesini (Örn: "emeyl") engelliyoruz.
    public enum NotificationType {
        EMAIL,
        SMS,
        PUSH_NOTIFICATION, // Mobil uygulama bildirimi
        SYSTEM_ALERT       // Sadece Loglara veya Admin ekranına düşecek kırmızı alarmlar
    }
}