package com.mustafa.service.transfer;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.ICurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyTransferService {

    private final IAccountRepository accountRepository;
    private final ITransactionRepository transactionRepository;
    private final ICurrencyService currencyService;
    private final RabbitMQPublisher rabbitPublisher;

    // The calling use case owns the transaction, including the entire payroll batch.
    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionResponse execute(TransferCommand command, TransferPurpose purpose) {
        Account sender = accountRepository.findByIban(command.senderIban())
                .orElseThrow(() -> new BankOperationException("Gönderen hesap bulunamadı!"));
        Account receiver = accountRepository.findByIban(command.receiverIban())
                .orElseThrow(() -> new BankOperationException("Alıcı hesap bulunamadı!"));

        if (!sender.getOwnerIdentityNumber().equals(command.ownerIdentityNumber())) {
            throw new BankOperationException("Sadece kendi hesaplarınızdan para transferi yapabilirsiniz!");
        }
        if (sender.getBalance().compareTo(command.amount()) < 0) {
            throw new BankOperationException("Yetersiz bakiye!");
        }
        if (sender.getIban().equals(receiver.getIban())) {
            throw new BankOperationException("Aynı hesaba transfer yapamazsınız.");
        }
        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankOperationException("Transfer tutarı 0'dan büyük olmalıdır!");
        }
        if (!sender.isActive() || !receiver.isActive()) {
            throw new BankOperationException("İşlem yapılacak hesaplardan biri kapalıdır!");
        }

        BigDecimal converted = BigDecimal.valueOf(currencyService.convertAmount(
                command.amount().doubleValue(), sender.getCurrency().name(), receiver.getCurrency().name()));
        BigDecimal amountInTry = sender.getCurrency() == Account.Currency.TRY ? command.amount()
                : BigDecimal.valueOf(currencyService.convertAmount(
                        command.amount().doubleValue(), sender.getCurrency().name(), "TRY"));
        String description = command.description() != null ? command.description() : "Para Transferi";
        sender.setBalance(sender.getBalance().subtract(command.amount()));

        Transaction.TransactionStatus status;
        if (purpose.requiresApproval(amountInTry)) {
            status = Transaction.TransactionStatus.PENDING_APPROVAL;
            description += " - [YÜKLÜ İŞLEM: YÖNETİCİ ONAYI BEKLİYOR]";
            accountRepository.save(sender);
        } else {
            status = Transaction.TransactionStatus.COMPLETED;
            receiver.setBalance(receiver.getBalance().add(converted));
            accountRepository.save(sender);
            accountRepository.save(receiver);
        }
        Transaction transaction = Transaction.builder()
                .referenceNo(UUID.randomUUID().toString()).senderAccount(sender).receiverAccount(receiver)
                .amount(command.amount()).convertedAmount(converted)
                // Preserve the existing API/history classification for payroll payments.
                .transactionType(Transaction.TransactionType.TRANSFER)
                .status(status).description(description).build();
        transactionRepository.save(transaction);

        String maskedIdentity = maskIdentity(command.ownerIdentityNumber());
        if (status == Transaction.TransactionStatus.PENDING_APPROVAL) {
            rabbitPublisher.sendNotification(NotificationMessage.builder()
                    .destination("admin@bank.com").subject("🚨 MASAK LİMİTİ AŞILDI")
                    .content("Yüklü işlem onayı bekliyor. Ref: " + transaction.getReferenceNo())
                    .identityNumber(maskedIdentity)
                    .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT).build());
        } else {
            rabbitPublisher.sendNotification(NotificationMessage.builder()
                    .destination(sender.getOwnerIdentityNumber()).subject("Para Transferi Başarılı")
                    .content("Transferiniz gerçekleşti.").identityNumber(maskedIdentity)
                    .notificationType(NotificationMessage.NotificationType.EMAIL).build());
        }
        log.info("Transfer kaydı hazırlandı. Ref: {}, Akış: {}, Durum: {}",
                transaction.getReferenceNo(), purpose, status);
        return TransactionResponse.builder()
                .referenceNo(transaction.getReferenceNo()).amount(transaction.getAmount())
                .convertedAmount(transaction.getConvertedAmount()).transactionType(transaction.getTransactionType())
                .status(transaction.getStatus()).description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .senderAccountId(sender.getId()).receiverAccountId(receiver.getId()).build();
    }

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }
}
