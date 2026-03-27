package com.mustafa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "company_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    // Şirket ve Çalışan aynı serviste olduğu için bu bağ (ManyToOne) kalabilir!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // 🚀 BÜYÜK DEĞİŞİM: RetailCustomer nesnesi YERİNE sadece personelin TC Numarası!
    @Column(name = "employee_identity_number", nullable = false, length = 11)
    private String employeeIdentityNumber;

    @Column(nullable = false)
    private BigDecimal salaryAmount;

    @Column(nullable = false, length = 26)
    private String salaryIban;
}