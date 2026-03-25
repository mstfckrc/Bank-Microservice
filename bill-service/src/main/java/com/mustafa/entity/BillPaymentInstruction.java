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

    // 🚀 MİKROSERVİS DEVRİMİ: Artık Account veya AppUser tablolarına fiziksel bağ YOK!
    @Column(name = "identity_number", nullable = false, length = 11)
    private String identityNumber; // Müşterinin kimliği

    @Column(name = "account_id", nullable = false)
    private Long accountId; // Karargahtaki kasanın sadece ID'si

    @Column(nullable = false, length = 26)
    private String iban; // Ön yüzde göstermek için sakladığımız IBAN

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_type", nullable = false, length = 20)
    private BillType billType;

    @Column(name = "subscriber_no", nullable = false, length = 50)
    private String subscriberNo;

    @Column(name = "payment_day", nullable = false)
    private Integer paymentDay;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public enum BillType {
        ELECTRICITY, WATER, INTERNET, GAS
    }
}