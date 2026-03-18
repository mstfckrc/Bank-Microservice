package com.mustafa.dto.response;

import com.mustafa.entity.Account.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String iban;
    private BigDecimal balance;
    private Currency currency;
    private boolean isActive;

    private String ownerName; // Şirket ise şirket adı, birey ise ad soyad
    private String identityNumber; // TC veya Vergi No
}