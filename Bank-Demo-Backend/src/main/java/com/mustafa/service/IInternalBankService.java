package com.mustafa.service;

import com.mustafa.dto.response.AccountValidationResponse;

public interface IInternalBankService {

    AccountValidationResponse validateAccount(String iban);

    // 🚀 YENİ EKLENDİ: Müşteri hesaplarını silme emri
    void deleteCustomerAccounts(String identityNumber);
}
