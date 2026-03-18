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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retail_customer_id", nullable = false)
    private RetailCustomer retailCustomer;

    @Column(nullable = false)
    private BigDecimal salaryAmount;

    @Column(nullable = false, length = 26)
    private String salaryIban;
}