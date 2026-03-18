package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String accountNumber; // Örn: 1029384756

    @Column(unique = true, nullable = false, length = 26)
    private String iban; // Örn: TR12 0009 9000 0010 2938 4756 01

    @Builder.Default // Yeni hesap açıldığında bakiye varsayılan olarak 0 başlasın
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency; // TRY, USD, EUR

    // İLİŞKİ: Her hesabın mutlak bir sahibi (Müşterisi) olmak zorundadır
    // Performans için veritabanından hesap çekilirken müşteriyi hemen getirme (LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    // Bu hesaptan çıkan paralar (Gönderilenler)
    @OneToMany(mappedBy = "senderAccount")
    private List<Transaction> sentTransactions;

    // Bu hesaba giren paralar (Alınanlar)
    @OneToMany(mappedBy = "receiverAccount")
    private List<Transaction> receivedTransactions;

    // ... mevcut kodların (iban, balance vb.) altına ekle ...

    @Builder.Default
    @Column(nullable = false , columnDefinition = "boolean default true")
    private boolean isActive = true; // Soft delete için (Hesap kapatıldığında false olacak)

    // --- PARA BİRİMLERİ ---
    public enum Currency {
        TRY, USD, EUR
    }
}