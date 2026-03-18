package com.mustafa.service;

import com.mustafa.dto.request.DepositRequest;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;

import java.util.List;

public interface ITransactionService {

    // Hesaba dışarıdan (ATM) para yatırma
    TransactionResponse deposit(DepositRequest request);

    // İki hesap arası para transferi (Havale/EFT)
    TransactionResponse transfer(TransferRequest request);


    List<TransactionResponse> getAccountTransactions(
            String accountNumber,
            String type,
            String startDate,
            String endDate
    );

    // com.mustafa.service.ITransactionService.java içine ekle:
    List<TransactionResponse> getAllTransactionsForAdmin(String status);
    TransactionResponse approveTransaction(String referenceNo);
    TransactionResponse rejectTransaction(String referenceNo);
}