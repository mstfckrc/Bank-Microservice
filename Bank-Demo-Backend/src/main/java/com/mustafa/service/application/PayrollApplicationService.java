package com.mustafa.service.application;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.request.SalaryPaymentItem;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.transfer.MoneyTransferService;
import com.mustafa.service.transfer.TransferCommand;
import com.mustafa.service.transfer.TransferPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollApplicationService {
    private final IAccountRepository accountRepository;
    private final ICurrencyService currencyService;
    private final MoneyTransferService moneyTransferService;

    // One Core DB transaction for the whole batch; employee transfers must join it.
    @Transactional
    public List<TransactionResponse> payBulkSalaries(BulkSalaryRequest request) {
        Account sender = accountRepository.findByIban(request.getSenderIban())
                .orElseThrow(() -> new BankOperationException("Çıkış kasası bulunamadı!"));
        if (!sender.isActive()) throw new BankOperationException("Çıkış kasası pasif durumdadır!");

        String identity = SecurityContextHolder.getContext().getAuthentication().getName();
        BigDecimal totalSalaryInTry = request.getSalaryItems().stream()
                .map(SalaryPaymentItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean foreignSender = sender.getCurrency() != Account.Currency.TRY;
        BigDecimal totalRequired = foreignSender
                ? BigDecimal.valueOf(currencyService.convertAmount(
                        totalSalaryInTry.doubleValue(), "TRY", sender.getCurrency().name()))
                : totalSalaryInTry;
        if (sender.getBalance().compareTo(totalRequired) < 0) {
            throw new BankOperationException("Kasada yeterli bakiye yok! Gereken: "
                    + totalRequired + " " + sender.getCurrency().name());
        }

        List<TransactionResponse> results = new ArrayList<>();
        for (SalaryPaymentItem item : request.getSalaryItems()) {
            BigDecimal amount = foreignSender
                    ? BigDecimal.valueOf(currencyService.convertAmount(
                            item.getAmount().doubleValue(), "TRY", sender.getCurrency().name()))
                    : item.getAmount();
            String description = "Maaş Ödemesi - " + request.getCompanyName();
            if (foreignSender) description += " (Orijinal: " + item.getAmount() + " TRY)";
            TransferCommand command = new TransferCommand(sender.getIban(), item.getReceiverIban(),
                    amount, description, identity);
            results.add(moneyTransferService.execute(command, TransferPurpose.SALARY));
        }
        return results;
    }
}
