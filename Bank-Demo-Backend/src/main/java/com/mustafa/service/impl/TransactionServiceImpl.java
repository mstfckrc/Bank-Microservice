package com.mustafa.service.impl;

import com.mustafa.dto.message.NotificationMessage;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.dto.request.DepositRequest;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Account;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.ITransactionService;
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

@Slf4j // 🚀 LOGGER AKTİF
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

    private final IAccountRepository accountRepository;
    private final ITransactionRepository transactionRepository;
    private final ICurrencyService currencyService;

    private final RabbitMQPublisher rabbitPublisher;

    private static final BigDecimal TRANSACTION_LIMIT = new BigDecimal("50000");

    // 🚀 KVKK Maskeleme Kalkanı
    private String maskIdentity(String identity) {
        if (identity == null || identity.length() <= 4) return "****";
        return "*******" + identity.substring(identity.length() - 4);
    }

    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);
        log.info("Para yatırma işlemi başlatıldı. Kullanıcı: {}, IBAN: {}, Tutar: {}", maskedId, request.getIban(), request.getAmount());

        // 🚀 DÜZELTME: Hesap bulunamazsa artık sessizce çökmek yerine loga (Telsize) haber verecek!
        Account account = accountRepository.findByIban(request.getIban())
                .orElseThrow(() -> {
                    log.warn("Para yatırma reddedildi: Belirtilen hedef hesap (IBAN: {}) sistemde bulunamadı!", request.getIban());
                    return new BankOperationException("Hesap bulunamadı!");
                });

        if (!account.getAppUser().getIdentityNumber().equals(currentIdentity)) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Kullanıcı ({}), başkasına ait bir hesaba para yatırmaya çalıştı!", maskedId);
            throw new BankOperationException("Sadece kendi hesaplarınıza para yatırabilirsiniz!");
        }

        if (account.getAppUser().getStatus() != AppUser.ApprovalStatus.APPROVED) {
            log.warn("İşlem reddedildi: Kullanıcı ({}) onaylı değil (PENDING).", maskedId);
            throw new BankOperationException("Hesabınız onaylı olmadığı için para yatırma işlemi yapamazsınız. Lütfen durumunuzu kontrol ediniz.");
        }

        if (!account.isActive()) {
            log.warn("İşlem reddedildi: Hedef hesap ({}) pasif durumda.", request.getIban());
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
        log.info("✅ Para yatırma işlemi başarıyla tamamlandı. Referans: {}", transaction.getReferenceNo());

        NotificationMessage notification = NotificationMessage.builder()
                .destination(account.getAppUser().getIdentityNumber()) // Şimdilik kimlik no veriyoruz, ileride email çekilebilir
                .subject("Hesaba Para Yatırma İşlemi")
                .content(String.format("Hesabınıza (IBAN: %s) %s %s tutarında para yatırılmıştır. Ref: %s",
                        request.getIban(), request.getAmount(), account.getCurrency(), transaction.getReferenceNo()))
                .identityNumber(maskedId)
                .notificationType(NotificationMessage.NotificationType.PUSH_NOTIFICATION) // Mobil bildirim tipi
                .build();

        rabbitPublisher.sendNotification(notification);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        String maskedId = maskIdentity(currentIdentity);

        log.info("Transfer süreci başlatıldı. Gönderen: {}, Alıcı IBAN: {}, Tutar: {}", maskedId, request.getReceiverIban(), request.getAmount());

        // 🚀 DÜZELTME: Gönderen hesap hatası loglandı
        Account senderAccount = accountRepository.findByIban(request.getSenderIban())
                .orElseThrow(() -> {
                    log.warn("Transfer iptali: Gönderici IBAN ({}) veritabanında bulunamadı!", request.getSenderIban());
                    return new BankOperationException("Gönderen hesap bulunamadı!");
                });

        // 🚀 DÜZELTME: Alıcı hesap (Örn: "askjdaskd") hatası loglandı
        Account receiverAccount = accountRepository.findByIban(request.getReceiverIban())
                .orElseThrow(() -> {
                    log.warn("Transfer iptali: Hedef alınan alıcı IBAN ({}) sistemde kayıtlı değil!", request.getReceiverIban());
                    return new BankOperationException("Alıcı hesap bulunamadı!");
                });

        if (!senderAccount.getAppUser().getIdentityNumber().equals(currentIdentity)) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Kullanıcı ({}), başkasına ait bir kasadan transfer yapmaya çalıştı!", maskedId);
            throw new BankOperationException("Sadece kendi hesaplarınızdan para transferi yapabilirsiniz!");
        }

        if (senderAccount.getAppUser().getStatus() != AppUser.ApprovalStatus.APPROVED) {
            log.warn("Transfer reddedildi: Kullanıcı ({}) onaylı değil.", maskedId);
            throw new BankOperationException("Hesabınız onaylı olmadığı için para transferi gerçekleştiremezsiniz. Lütfen durumunuzu kontrol ediniz.");
        }

        if (senderAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Transfer reddedildi: Yetersiz Bakiye! Kasa: {}, İstenen: {}, Mevcut: {}",
                    senderAccount.getIban(), request.getAmount(), senderAccount.getBalance());
            throw new BankOperationException("Yetersiz bakiye! İşlem gerçekleştirilemedi.");
        }

        if (senderAccount.getIban().equals(receiverAccount.getIban())) {
            log.warn("Transfer iptali: Kullanıcı kendi hesabından aynı hesabına para göndermeye çalıştı.");
            throw new BankOperationException("Aynı hesaba transfer yapamazsınız. Lütfen farklı bir alıcı IBAN giriniz.");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankOperationException("Transfer tutarı 0'dan büyük olmalıdır!");
        }

        if (!senderAccount.isActive() || !receiverAccount.isActive()) {
            log.warn("Transfer reddedildi: Gönderici veya Alıcı hesap pasif durumda.");
            throw new BankOperationException("İşlem yapılacak hesaplardan biri kapalıdır!");
        }

        // 1. Gönderenden parayı DÜŞ
        senderAccount.setBalance(senderAccount.getBalance().subtract(request.getAmount()));

        // 2. Canlı Kur Çevirimi
        Double convertedAmountDouble = currencyService.convertAmount(
                request.getAmount().doubleValue(),
                senderAccount.getCurrency().toString(),
                receiverAccount.getCurrency().toString()
        );
        BigDecimal convertedAmount = BigDecimal.valueOf(convertedAmountDouble);

        // 🚀 ÇÖZÜM: Sadece para birimleri FARKLIYSA çevrim metnini ekle!
        String enrichedDescription = request.getDescription() != null ? request.getDescription() : "Para Transferi";

        if (senderAccount.getCurrency() != receiverAccount.getCurrency()) {
            enrichedDescription += " (Çevrim: " + request.getAmount() + " " + senderAccount.getCurrency() +
                    " -> " + String.format("%.2f", convertedAmountDouble) + " " + receiverAccount.getCurrency() + ")";
        }

        // 3. MASAK KONTROLÜ VE İŞLEM TİPİ BELİRLEME
        Transaction.TransactionStatus status;

        // 🚀 DÜZELTME: Eğer para zaten TRY ise döviz motorunu hiç yorma, değilse çevir!
        Double amountInTryDouble;
        if (senderAccount.getCurrency().toString().equals("TRY")) {
            amountInTryDouble = request.getAmount().doubleValue();
        } else {
            amountInTryDouble = currencyService.convertAmount(
                    request.getAmount().doubleValue(),
                    senderAccount.getCurrency().toString(),
                    "TRY"
            );
        }

        BigDecimal amountInTry = BigDecimal.valueOf(amountInTryDouble);

        Transaction.TransactionType type = Transaction.TransactionType.TRANSFER;

        if (request.isSalaryPayment()) {
            log.info("🛡️ Maaş ödemesi bayrağı tespit edildi! İşlem MASAK limit denetiminden ({} TRY) muaf tutuluyor.", TRANSACTION_LIMIT);
        }

        if (!request.isSalaryPayment() && amountInTry.compareTo(TRANSACTION_LIMIT) >= 0) {
            log.warn("🚨 MASAK LİMİTİ AŞILDI! İşlem büyüklüğü: ~{} TRY. İşlem Admin onayına (PENDING_APPROVAL) bekletilmek üzere donduruldu.", String.format("%.2f", amountInTryDouble));
            accountRepository.save(senderAccount);
            status = Transaction.TransactionStatus.PENDING_APPROVAL;
            enrichedDescription += String.format(" - [YÜKLÜ İŞLEM: Yaklaşık %.2f TL - YÖNETİCİ ONAYI BEKLİYOR]", amountInTryDouble);
        } else {
            log.info("İşlem limitler dahilinde. Otomatik transfer onayı verildi.");
            receiverAccount.setBalance(receiverAccount.getBalance().add(convertedAmount));
            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);
            status = Transaction.TransactionStatus.COMPLETED;
        }

        // 4. İşlemi Kaydet
        Transaction transaction = Transaction.builder()
                .referenceNo(UUID.randomUUID().toString())
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .transactionType(type)
                .status(status)
                .description(enrichedDescription)
                .build();

        transactionRepository.save(transaction);
        log.info("✅ İşlem kaydı başarıyla oluşturuldu. Durum: {}, Referans: {}", status, transaction.getReferenceNo());

        if (status == Transaction.TransactionStatus.PENDING_APPROVAL) {
            // Adminlere Kırmızı Alarm!
            NotificationMessage alertMessage = NotificationMessage.builder()
                    .destination("admin@bank.com") // Sistem yöneticisine gidiyor
                    .subject("🚨 DİKKAT: MASAK LİMİTİ AŞILDI")
                    .content(String.format("Yüklü işlem onayı bekliyor! Gönderen: %s, Tutar: %s, Ref: %s",
                            request.getSenderIban(), request.getAmount(), transaction.getReferenceNo()))
                    .identityNumber(maskedId)
                    .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT)
                    .build();
            rabbitPublisher.sendNotification(alertMessage);

        } else {
            // Müşteriye Normal Bilgilendirme
            NotificationMessage successMessage = NotificationMessage.builder()
                    .destination(senderAccount.getAppUser().getIdentityNumber())
                    .subject("Para Transferi Başarılı")
                    .content(String.format("Alıcıya (%s) %s %s tutarındaki transferiniz başarıyla gerçekleşti.",
                            request.getReceiverIban(), request.getAmount(), senderAccount.getCurrency()))
                    .identityNumber(maskedId)
                    .notificationType(NotificationMessage.NotificationType.EMAIL)
                    .build();
            rabbitPublisher.sendNotification(successMessage);
        }

        return mapToResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getAccountTransactions(String accountNumber, String type, String startDate, String endDate) {

        // 🚀 DÜZELTME: Ekstre çekerken hesap bulunamazsa logla
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Ekstre iptali: Sorgulanan hesap numarası ({}) bulunamadı!", accountNumber);
                    return new BankOperationException("Hesap bulunamadı!");
                });

        String currentIdentity = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!account.getAppUser().getIdentityNumber().equals(currentIdentity)) {
            log.warn("🚨 GÜVENLİK İHLALİ DENEMESİ! Kullanıcı ({}), başkasına ait {} numaralı hesabın ekstresine erişmeye çalıştı!", maskIdentity(currentIdentity), accountNumber);
            throw new BankOperationException("Sadece kendi hesaplarınızın hareketlerini görebilirsiniz!");
        }

        log.info("Hesap ekstresi çekiliyor. Hesap No: {}, Filtreler -> Tip: {}, Tarih: {} - {}", accountNumber, type, startDate, endDate);

        List<Transaction> transactions = transactionRepository
                .findBySenderAccountIdOrReceiverAccountIdOrderByTransactionDateDesc(account.getId(), account.getId());

        Stream<Transaction> stream = transactions.stream();

        if (type != null && !type.isBlank()) {
            stream = stream.filter(t -> t.getTransactionType().name().equalsIgnoreCase(type));
        }
        if (startDate != null && !startDate.isBlank()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            stream = stream.filter(t -> !t.getTransactionDate().isBefore(start));
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            stream = stream.filter(t -> !t.getTransactionDate().isAfter(end));
        }

        return stream.map(this::mapToResponse).collect(Collectors.toList());
    }

    // --- ADMİN METOTLARI ---

    @Override
    public List<TransactionResponse> getAllTransactionsForAdmin(String status) {
        log.info("Admin İşlemi: Sistemdeki tüm transfer kayıtları sorgulanıyor. Filtre: {}", status != null ? status : "TÜMÜ");
        Stream<Transaction> stream = transactionRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));

        if (status != null && !status.isBlank()) {
            stream = stream.filter(t -> t.getStatus().name().equalsIgnoreCase(status));
        }
        return stream.map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionResponse approveTransaction(String referenceNo) {
        log.info("Admin İşlemi: MASAK limitine takılan işlem ({}) için ONAY süreci başlatıldı...", referenceNo);

        // 🚀 DÜZELTME: Admin işlem bulamazsa logla
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> {
                    log.warn("Admin Onay Hatası: Onaylanmak istenen işlem ({}) bulunamadı!", referenceNo);
                    return new BankOperationException("İşlem bulunamadı!");
                });

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING_APPROVAL) {
            log.warn("Admin Onay Hatası: İşlem ({}) zaten onaylanmış veya reddedilmiş!", referenceNo);
            throw new BankOperationException("Bu işlem onay bekleyen statüde değil!");
        }

        Account receiver = transaction.getReceiverAccount();
        receiver.setBalance(receiver.getBalance().add(transaction.getConvertedAmount()));
        accountRepository.save(receiver);

        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        String updatedDesc = transaction.getDescription().replace(" - [YÜKLÜ İŞLEM: YÖNETİCİ ONAYI BEKLİYOR]", "") + " - [ONAYLANDI]";
        transaction.setDescription(updatedDesc);
        transactionRepository.save(transaction);

        log.info("✅ Admin İşlemi Başarılı: İşlem ({}) ONAYLANDI ve para alıcı hesaba geçirildi.", referenceNo);

        NotificationMessage approvalMessage = NotificationMessage.builder()
                .destination(transaction.getSenderAccount().getAppUser().getIdentityNumber())
                .subject("✅ İşleminiz Onaylandı")
                .content("Bekleyen yüklü transfer işleminiz yönetici tarafından onaylanmış ve alıcıya ulaşmıştır. Ref: " + referenceNo)
                .identityNumber(maskIdentity(transaction.getSenderAccount().getAppUser().getIdentityNumber()))
                .notificationType(NotificationMessage.NotificationType.EMAIL)
                .build();

        rabbitPublisher.sendNotification(approvalMessage);
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse rejectTransaction(String referenceNo) {
        log.info("Admin İşlemi: MASAK limitine takılan işlem ({}) için RED süreci başlatıldı...", referenceNo);

        // 🚀 DÜZELTME: Admin işlem bulamazsa logla
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> {
                    log.warn("Admin Ret Hatası: Reddedilmek istenen işlem ({}) bulunamadı!", referenceNo);
                    return new BankOperationException("İşlem bulunamadı!");
                });

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING_APPROVAL) {
            log.warn("Admin Ret Hatası: İşlem ({}) zaten onaylanmış veya reddedilmiş!", referenceNo);
            throw new BankOperationException("Bu işlem onay bekleyen statüde değil!");
        }

        Account sender = transaction.getSenderAccount();
        sender.setBalance(sender.getBalance().add(transaction.getAmount()));
        accountRepository.save(sender);

        transaction.setStatus(Transaction.TransactionStatus.REJECTED);
        String updatedDesc = transaction.getDescription().replace(" - [YÜKLÜ İŞLEM: YÖNETİCİ ONAYI BEKLİYOR]", "") + " - [REDDEDİLDİ VE İADE EDİLDİ]";
        transaction.setDescription(updatedDesc);
        transactionRepository.save(transaction);

        log.info("🚫 Admin İşlemi Başarılı: İşlem ({}) REDDEDİLDİ ve dondurulan tutar göndericiye iade edildi.", referenceNo);

        NotificationMessage rejectMessage = NotificationMessage.builder()
                .destination(transaction.getSenderAccount().getAppUser().getIdentityNumber())
                .subject("🚫 İşleminiz Reddedildi")
                .content("Bekleyen işleminiz MASAK/Güvenlik kuralları gereği reddedilmiş olup, tutar kasanıza iade edilmiştir. Ref: " + referenceNo)
                .identityNumber(maskIdentity(transaction.getSenderAccount().getAppUser().getIdentityNumber()))
                .notificationType(NotificationMessage.NotificationType.SYSTEM_ALERT) // Veya EMAIL
                .build();

        rabbitPublisher.sendNotification(rejectMessage);

        return mapToResponse(transaction);
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