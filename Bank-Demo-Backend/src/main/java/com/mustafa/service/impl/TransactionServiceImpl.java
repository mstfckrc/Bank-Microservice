package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.dto.request.InternalPaymentRequest;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.DepositRequest;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.ITransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

    private final IAccountRepository accountRepository;
    private final ITransactionRepository transactionRepository;
    private final ICurrencyService currencyService;
    private final RabbitMQPublisher rabbitPublisher;

    // 🚀 YENİ MİMARİ: Statüyü Header'dan okumak için Request nesnesini içeri alıyoruz
    private final HttpServletRequest httpRequest;

    private static final BigDecimal TRANSACTION_LIMIT = new BigDecimal("50000");

    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    // 🚀 YENİ MİMARİ: Gateway'in getirdiği X-User-Status başlığını okuyan metot
    private void checkUserApprovalStatus() {
        String status = httpRequest.getHeader("X-User-Status");
        if (status == null || !status.equals("APPROVED")) {
            throw new BankOperationException("Hesabınız yönetici tarafından onaylanmadığı (PENDING) için işlem yapamazsınız!");
        }
    }

    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);

        // 1. STATÜ KONTROLÜ (Artık DB'den değil, Gateway'den gelen Token Header'ından soruluyor)
        checkUserApprovalStatus();

        Account account = accountRepository.findByIban(request.getIban())
                .orElseThrow(() -> new BankOperationException("Hesap bulunamadı!"));

        if (!account.getOwnerIdentityNumber().equals(currentIdentity)) {
            throw new BankOperationException("Sadece kendi hesaplarınıza para yatırabilirsiniz!");
        }

        if (!account.isActive()) {
            throw new BankOperationException("Bu hesap kapalı olduğu için para yatırma işlemi yapılamaz!");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .referenceNo(UUID.randomUUID().toString())
                .receiverAccount(account)
                .amount(request.getAmount())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Kendi Hesabına Para Yatırma")
                .build();
        transactionRepository.save(transaction);

        NotificationMessage notification = NotificationMessage.builder()
                .destination(account.getOwnerIdentityNumber())
                .subject("Hesaba Para Yatırma")
                .content(String.format("Hesabınıza %s %s yatırılmıştır.", request.getAmount(), account.getCurrency()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.PUSH_NOTIFICATION)
                .build();
        rabbitPublisher.sendNotification(notification);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);

        // 1. STATÜ KONTROLÜ
        checkUserApprovalStatus();

        Account senderAccount = accountRepository.findByIban(request.getSenderIban())
                .orElseThrow(() -> new BankOperationException("Gönderen hesap bulunamadı!"));
        Account receiverAccount = accountRepository.findByIban(request.getReceiverIban())
                .orElseThrow(() -> new BankOperationException("Alıcı hesap bulunamadı!"));

        if (!senderAccount.getOwnerIdentityNumber().equals(currentIdentity)) {
            throw new BankOperationException("Sadece kendi hesaplarınızdan para transferi yapabilirsiniz!");
        }

        if (senderAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BankOperationException("Yetersiz bakiye!");
        }

        if (senderAccount.getIban().equals(receiverAccount.getIban())) {
            throw new BankOperationException("Aynı hesaba transfer yapamazsınız.");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankOperationException("Transfer tutarı 0'dan büyük olmalıdır!");
        }

        if (!senderAccount.isActive() || !receiverAccount.isActive()) {
            throw new BankOperationException("İşlem yapılacak hesaplardan biri kapalıdır!");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(request.getAmount()));

        Double convertedAmountDouble = currencyService.convertAmount(
                request.getAmount().doubleValue(),
                senderAccount.getCurrency().toString(),
                receiverAccount.getCurrency().toString()
        );
        BigDecimal convertedAmount = BigDecimal.valueOf(convertedAmountDouble);

        String enrichedDescription = request.getDescription() != null ? request.getDescription() : "Para Transferi";

        Double amountInTryDouble = senderAccount.getCurrency().toString().equals("TRY") ?
                request.getAmount().doubleValue() :
                currencyService.convertAmount(request.getAmount().doubleValue(), senderAccount.getCurrency().toString(), "TRY");

        BigDecimal amountInTry = BigDecimal.valueOf(amountInTryDouble);

        Transaction.TransactionStatus status;
        if (!request.isSalaryPayment() && amountInTry.compareTo(TRANSACTION_LIMIT) >= 0) {
            accountRepository.save(senderAccount);
            status = Transaction.TransactionStatus.PENDING_APPROVAL;
            enrichedDescription += " - [YÜKLÜ İŞLEM: YÖNETİCİ ONAYI BEKLİYOR]";
        } else {
            receiverAccount.setBalance(receiverAccount.getBalance().add(convertedAmount));
            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);
            status = Transaction.TransactionStatus.COMPLETED;
        }

        Transaction transaction = Transaction.builder()
                .referenceNo(UUID.randomUUID().toString())
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .transactionType(Transaction.TransactionType.TRANSFER)
                .status(status)
                .description(enrichedDescription)
                .build();
        transactionRepository.save(transaction);

        if (status == Transaction.TransactionStatus.PENDING_APPROVAL) {
            rabbitPublisher.sendNotification(NotificationMessage.builder()
                    .destination("admin@bank.com")
                    .subject("🚨 MASAK LİMİTİ AŞILDI")
                    .content("Yüklü işlem onayı bekliyor. Ref: " + transaction.getReferenceNo())
                    .identityNumber(maskedId)
                    .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT).build());
        } else {
            rabbitPublisher.sendNotification(NotificationMessage.builder()
                    .destination(senderAccount.getOwnerIdentityNumber())
                    .subject("Para Transferi Başarılı")
                    .content("Transferiniz gerçekleşti.")
                    .identityNumber(maskedId)
                    .notificationType(NotificationMessage.NotificationType.EMAIL).build());
        }
        return mapToResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getAccountTransactions(String accountNumber, String type, String startDate, String endDate) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankOperationException("Hesap bulunamadı!"));
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!account.getOwnerIdentityNumber().equals(currentIdentity)) {
            throw new BankOperationException("Sadece kendi hesaplarınızın hareketlerini görebilirsiniz!");
        }

        List<Transaction> transactions = transactionRepository.findBySenderAccountIdOrReceiverAccountIdOrderByTransactionDateDesc(account.getId(), account.getId());
        Stream<Transaction> stream = transactions.stream();

        if (type != null && !type.isBlank()) stream = stream.filter(t -> t.getTransactionType().name().equalsIgnoreCase(type));
        if (startDate != null && !startDate.isBlank()) stream = stream.filter(t -> !t.getTransactionDate().isBefore(LocalDate.parse(startDate).atStartOfDay()));
        if (endDate != null && !endDate.isBlank()) stream = stream.filter(t -> !t.getTransactionDate().isAfter(LocalDate.parse(endDate).atTime(23, 59, 59)));

        return stream.map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getAllTransactionsForAdmin(String status) {
        Stream<Transaction> stream = transactionRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));
        if (status != null && !status.isBlank()) stream = stream.filter(t -> t.getStatus().name().equalsIgnoreCase(status));
        return stream.map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionResponse approveTransaction(String referenceNo) {
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new BankOperationException("İşlem bulunamadı!"));
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING_APPROVAL) throw new BankOperationException("İşlem onay bekleyen statüde değil!");

        Account receiver = transaction.getReceiverAccount();
        receiver.setBalance(receiver.getBalance().add(transaction.getConvertedAmount()));
        accountRepository.save(receiver);

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(transaction.getDescription().replace(" - [YÜKLÜ İŞLEM: YÖNETİCİ ONAYI BEKLİYOR]", "") + " - [ONAYLANDI]");
        transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse rejectTransaction(String referenceNo) {
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new BankOperationException("İşlem bulunamadı!"));
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING_APPROVAL) throw new BankOperationException("İşlem onay bekleyen statüde değil!");

        Account sender = transaction.getSenderAccount();
        sender.setBalance(sender.getBalance().add(transaction.getAmount()));
        accountRepository.save(sender);

        transaction.setStatus(Transaction.TransactionStatus.REJECTED);
        transaction.setDescription(transaction.getDescription().replace(" - [YÜKLÜ İŞLEM: YÖNETİCİ ONAYI BEKLİYOR]", "") + " - [REDDEDİLDİ VE İADE EDİLDİ]");
        transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public void processInternalPayment(String identityNumber, InternalPaymentRequest request) {
        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(() -> new BankOperationException("Kasa bulunamadı!"));
        if (!account.getOwnerIdentityNumber().equals(identityNumber)) throw new BankOperationException("Yetkisiz işlem: Kasa sahibi eşleşmiyor!");

        BigDecimal amountToDeduct = account.getCurrency().toString().equals("TRY") ? request.getAmount() :
                BigDecimal.valueOf(currencyService.convertAmount(request.getAmount().doubleValue(), "TRY", account.getCurrency().toString()));

        if (account.getBalance().compareTo(amountToDeduct) < 0) throw new BankOperationException("Kasada yeterli bakiye yok!");

        account.setBalance(account.getBalance().subtract(amountToDeduct));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .referenceNo(UUID.randomUUID().toString())
                .senderAccount(account)
                .amount(amountToDeduct)
                .convertedAmount(request.getAmount())
                .transactionType(Transaction.TransactionType.BILL_PAYMENT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();
        transactionRepository.save(transaction);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .referenceNo(transaction.getReferenceNo())
                .amount(transaction.getAmount())
                .convertedAmount(transaction.getConvertedAmount() != null ? transaction.getConvertedAmount() : transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .senderAccountId(transaction.getSenderAccount() != null ? transaction.getSenderAccount().getId() : null)
                .receiverAccountId(transaction.getReceiverAccount() != null ? transaction.getReceiverAccount().getId() : null)
                .build();
    }
}