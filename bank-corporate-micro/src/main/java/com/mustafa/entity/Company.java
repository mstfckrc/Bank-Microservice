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

    // 🚀 BÜYÜK DEĞİŞİM: AppUser nesnesi YERİNE sadece String TC/Vergi No!
    @Column(name = "company_identity_number", nullable = false, unique = true, length = 11)
    private String companyIdentityNumber;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String taxOffice;

    @Column(unique = true, nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "auto_salary_payment_enabled")
    @Builder.Default
    private Boolean autoSalaryPaymentEnabled = false;

    @Column(name = "salary_payment_day")
    private Integer salaryPaymentDay;

    @Column(name = "default_salary_iban", length = 34)
    private String defaultSalaryIban;

    public boolean isAutoSalaryPaymentEnabled() {
        return Boolean.TRUE.equals(this.autoSalaryPaymentEnabled);
    }
}