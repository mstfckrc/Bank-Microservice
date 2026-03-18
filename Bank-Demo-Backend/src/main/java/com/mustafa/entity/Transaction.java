package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Benzersiz bir dekont numarası (Örn: UUID olarak üreteceğiz)
    @Column(unique = true, nullable = false, updatable = false)
    private String referenceNo;

    // Gönderen Hesap (ATM'den nakit yatırmalarda null olabilir)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id")
    private Account senderAccount;

    // Alıcı Hesap (ATM'den nakit çekmelerde null olabilir)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_id")
    private Account receiverAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal convertedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    // 🚀 YENİ EKLENEN: İşlem Durumu (Eski kayıtlar için veritabanı seviyesinde varsayılan değer atadık)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'COMPLETED'")
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    // İşlem açıklaması (Örn: "Kira ödemesi", "Maaş avansı")
    @Column(length = 250)
    private String description;

    // İşlemin gerçekleştiği an (Biz set etmeyeceğiz, veritabanı otomatik atayacak)
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;


    // --- YENİ: İŞLEM DURUMLARI ---
    public enum TransactionStatus {
        COMPLETED,        // Onaylandı / Başarıyla Gerçekleşti (Eski işlemler veya 50.000 altı)
        PENDING_APPROVAL, // 50.000 TL ve üstü - Admin onayı bekliyor
        REJECTED          // Admin tarafından reddedildi ve iade edildi
    }

    // --- İŞLEM TİPLERİ ---
    public enum TransactionType {
        DEPOSIT,    // Para Yatırma
        WITHDRAWAL, // Para Çekme
        TRANSFER,   // Havale / EFT
        SALARY,     // MAAŞ
        BILL_PAYMENT // 🚀 YENİ EKLENEN: Fatura Ödemesi
    }
}