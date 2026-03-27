package com.mustafa.service;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.TransactionResponse;

import java.util.List;

public interface IInternalBankService {
    AccountValidationResponse validateAccount(String iban);
    CustomerProfileResponse getCustomerProfile(String identityNumber);
    List<TransactionResponse> payBulkSalaries(BulkSalaryRequest request);
}