package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bill_payment_instructions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPaymentInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Faturanın hangi hesaptan ödeneceği
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillType billType;

    // Örn: Elektrik faturası için tesisat numarası
    @Column(nullable = false, length = 50)
    private String subscriberNo;

    // Her ayın kaçında ödenecek? (1-31)
    @Column(nullable = false)
    private Integer paymentDay;

    // 🚀 GÜVENLİK: Aynı ay içinde faturayı 2 kere ödememek için son ödenme tarihi
    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    // Talimat açık mı kapalı mı?
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum BillType {
        ELECTRICITY, WATER, INTERNET, GAS
    }
}