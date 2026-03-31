package com.mustafa.service.impl;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.request.SalaryPaymentItem;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.IInternalBankService;
import com.mustafa.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalBankServiceImpl implements IInternalBankService {

    private final IAccountRepository accountRepository;
    private final ITransactionService transactionService;
    private final ICurrencyService currencyService;

    @Override
    public AccountValidationResponse validateAccount(String iban) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new BankOperationException("Kasa bulunamadı!"));

        return AccountValidationResponse.builder()
                .ownerIdentityNumber(account.getOwnerIdentityNumber()) // Sadece TC dönüyor!
                .isActive(account.isActive())
                .build();
    }

    // 🚀 BÜYÜK DEĞİŞİM: getCustomerProfile METODU SİLİNDİ!
    // Karargah müşteri tanımadığı için bu bilgi artık buradan verilemez.

    @Override
    @Transactional
    public List<TransactionResponse> payBulkSalaries(BulkSalaryRequest request) {
        Account senderAccount = accountRepository.findByIban(request.getSenderIban())
                .orElseThrow(() -> new BankOperationException("Çıkış kasası bulunamadı!"));

        if (!senderAccount.isActive()) {
            throw new BankOperationException("Çıkış kasası pasif durumdadır!");
        }

        BigDecimal totalSalaryInTry = request.getSalaryItems().stream()
                .map(SalaryPaymentItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRequiredInSenderCurrency = totalSalaryInTry;
        boolean isSenderCurrencyForeign = !senderAccount.getCurrency().name().equalsIgnoreCase("TRY");

        if (isSenderCurrencyForeign) {
            Double convertedTotal = currencyService.convertAmount(
                    totalSalaryInTry.doubleValue(), "TRY", senderAccount.getCurrency().name()
            );
            totalRequiredInSenderCurrency = BigDecimal.valueOf(convertedTotal);
            log.info("INTERNAL DÖVİZ: Toplam {} TRY maaş, {} {} olarak hesaplandı.",
                    totalSalaryInTry, String.format("%.2f", totalRequiredInSenderCurrency), senderAccount.getCurrency().name());
        }

        if (senderAccount.getBalance().compareTo(totalRequiredInSenderCurrency) < 0) {
            throw new BankOperationException("Kasada yeterli bakiye yok! Gereken: " + totalRequiredInSenderCurrency + " " + senderAccount.getCurrency().name());
        }

        List<TransactionResponse> transactionResults = new ArrayList<>();

        for (SalaryPaymentItem item : request.getSalaryItems()) {
            TransferRequest transferReq = new TransferRequest();
            transferReq.setSenderIban(senderAccount.getIban());
            transferReq.setReceiverIban(item.getReceiverIban());

            if (isSenderCurrencyForeign) {
                Double itemConverted = currencyService.convertAmount(
                        item.getAmount().doubleValue(), "TRY", senderAccount.getCurrency().name()
                );
                transferReq.setAmount(BigDecimal.valueOf(itemConverted));
                transferReq.setDescription("Maaş Ödemesi - " + request.getCompanyName() + " (Orijinal: " + item.getAmount() + " TRY)");
            } else {
                transferReq.setAmount(item.getAmount());
                transferReq.setDescription("Maaş Ödemesi - " + request.getCompanyName());
            }

            transferReq.setSalaryPayment(true);
            TransactionResponse response = transactionService.transfer(transferReq);
            transactionResults.add(response);
        }

        return transactionResults;
    }
}