package com.mustafa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEmployeeResponse {

    private Long id; // CompanyEmployee tablosundaki köprü kaydının ID'si
    private String identityNumber;
    private String firstName;
    private String lastName;
    private String salaryIban;
    private BigDecimal salaryAmount;
}