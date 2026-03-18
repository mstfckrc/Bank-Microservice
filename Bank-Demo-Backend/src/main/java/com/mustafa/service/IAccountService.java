package com.mustafa.service;

import com.mustafa.dto.request.CreateAccountRequest;
import com.mustafa.dto.response.AccountResponse;

import java.util.List;

public interface IAccountService {

    // Yeni hesap açma
    AccountResponse createAccount(CreateAccountRequest request);

    // Sisteme giren müşterinin kendi hesaplarını listelemesi
    List<AccountResponse> getMyAccounts();

    // Belirli bir hesap numarasına ait tekil hesap bilgisini getirir
    AccountResponse getAccountByAccountNumber(String accountNumber);

    // Hesabı güvenli bir şekilde kapatma (Soft Delete)
    void deleteAccount(String accountNumber);
}