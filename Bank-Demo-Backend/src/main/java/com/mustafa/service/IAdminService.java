package com.mustafa.service;

import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.SystemLogResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.dto.response.UserProfileResponse;

import java.util.List;

public interface IAdminService {
    List<AccountResponse> getAllAccounts();
    List<AccountResponse> getCustomerAccounts(String identityNumber);
    List<TransactionResponse> getAccountTransactions(String accountNumber);
    AccountResponse openAccountForCustomer(String identityNumber, OpenAccountRequest request);

    // 🚀 YENİ: İstihbarat (Log) Çekme Metodu
    List<SystemLogResponse> getSystemLogs(int limit, String level);
}