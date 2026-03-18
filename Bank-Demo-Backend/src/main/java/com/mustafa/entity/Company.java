package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "app_user_id", nullable = false, unique = true)
    private AppUser appUser;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String taxOffice;

    @Column(unique = true, nullable = false, length = 100)
    private String contactEmail;

    // 🚀 DÜZELTME 1: primitive 'boolean' yerine nesne olan 'Boolean' kullandık.
    // Böylece veritabanındaki eski kayıtların 'NULL' değerleri sistemi çökertmeyecek!
    @Column(name = "auto_salary_payment_enabled")
    @Builder.Default
    private Boolean autoSalaryPaymentEnabled = false;

    @Column(name = "salary_payment_day")
    private Integer salaryPaymentDay;

    @Column(name = "default_salary_iban", length = 34)
    private String defaultSalaryIban;

    // 🚀 DÜZELTME 2: Service katmanındaki `company.isAutoSalaryPaymentEnabled()`
    // çağrılarının hata vermemesi ve NullPointerException yemememiz için özel koruma metodu:
    public boolean isAutoSalaryPaymentEnabled() {
        return Boolean.TRUE.equals(this.autoSalaryPaymentEnabled);
    }
}