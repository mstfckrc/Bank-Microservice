package com.mustafa.service;

import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.dto.response.UserProfileResponse;

import java.util.List;

public interface IAdminService {
    List<UserProfileResponse> getAllCustomers();
    void deleteCustomer(String identityNumber);
    UserProfileResponse updateCustomer(String identityNumber, UpdateProfileRequest request);

    List<AccountResponse> getAllAccounts();
    List<AccountResponse> getCustomerAccounts(String identityNumber);
    List<TransactionResponse> getAccountTransactions(String accountNumber);
    AccountResponse openAccountForCustomer(String identityNumber, OpenAccountRequest request);

    void updateCustomerStatus(String identityNumber, String status);
}