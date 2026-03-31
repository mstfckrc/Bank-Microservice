package com.mustafa.service.impl;

import com.mustafa.dto.request.OpenAccountRequest;
import com.mustafa.dto.response.AccountResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final IAccountRepository accountRepository;
    private final ITransactionRepository transactionRepository;

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    private String getOwnerName(String identityNumber) {
        return "Müşteri (" + maskIdentity(identityNumber) + ")";
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(account -> AccountResponse.builder()
                        .id(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .currency(account.getCurrency())
                        .isActive(account.isActive())
                        .ownerName(getOwnerName(account.getOwnerIdentityNumber()))
                        .identityNumber(account.getOwnerIdentityNumber())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getCustomerAccounts(String identityNumber) {
        // 🚀 Kasa yoksa adam da yoktur mantığı.
        List<Account> accounts = accountRepository.findByOwnerIdentityNumber(identityNumber);

        return accounts.stream()
                .map(account -> AccountResponse.builder()
                        .id(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .currency(account.getCurrency())
                        .isActive(account.isActive())
                        .ownerName(getOwnerName(identityNumber))
                        .identityNumber(identityNumber)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getAccountTransactions(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankOperationException("Hesap bulunamadı!"));

        List<Transaction> transactions = transactionRepository
                .findBySenderAccountIdOrReceiverAccountIdOrderByTransactionDateDesc(account.getId(), account.getId());

        return transactions.stream().map(transaction -> TransactionResponse.builder()
                .referenceNo(transaction.getReferenceNo())
                .amount(transaction.getAmount())
                .convertedAmount(transaction.getConvertedAmount() != null ? transaction.getConvertedAmount() : transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .senderAccountId(transaction.getSenderAccount() != null ? transaction.getSenderAccount().getId() : null)
                .receiverAccountId(transaction.getReceiverAccount() != null ? transaction.getReceiverAccount().getId() : null)
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse openAccountForCustomer(String identityNumber, OpenAccountRequest request) {
        Account.Currency accountCurrency;
        try {
            if (request.getCurrency() == null || request.getCurrency().isBlank()) throw new IllegalArgumentException();
            accountCurrency = Account.Currency.valueOf(request.getCurrency().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BankOperationException("Geçersiz para birimi! Sadece TRY, USD veya EUR desteklenmektedir.");
        }

        String generatedAccountNumber = String.valueOf((long) (Math.random() * 9000000000L) + 1000000000L);
        String generatedIban = "TR" + "00000" + generatedAccountNumber + "000000001";

        Account newAccount = Account.builder()
                .ownerIdentityNumber(identityNumber)
                .accountNumber(generatedAccountNumber)
                .iban(generatedIban)
                .balance(java.math.BigDecimal.ZERO)
                .isActive(true)
                .currency(accountCurrency)
                .build();

        Account savedAccount = accountRepository.save(newAccount);

        return AccountResponse.builder()
                .id(savedAccount.getId())
                .accountNumber(savedAccount.getAccountNumber())
                .iban(savedAccount.getIban())
                .balance(savedAccount.getBalance())
                .currency(savedAccount.getCurrency())
                .isActive(savedAccount.isActive())
                .ownerName(getOwnerName(identityNumber))
                .identityNumber(identityNumber)
                .build();
    }
}