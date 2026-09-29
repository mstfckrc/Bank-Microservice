package com.mustafa.service.transfer;

import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.entity.Transaction;
import com.mustafa.dto.message.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTransferServiceTest {

    private final IAccountRepository accountRepository = mock(IAccountRepository.class);
    private final ITransactionRepository transactionRepository = mock(ITransactionRepository.class);
    private final ICurrencyService currencyService = mock(ICurrencyService.class);
    private final RabbitMQPublisher rabbitPublisher = mock(RabbitMQPublisher.class);

    private final MoneyTransferService service = new MoneyTransferService(
            accountRepository, transactionRepository, currencyService, rabbitPublisher);

    @Test
    void insufficientFundsStopsTransferBeforeAnySideEffect() {
        Account sender = Account.builder()
                .iban("SENDER")
                .ownerIdentityNumber("11111111111")
                .balance(new BigDecimal("10.00"))
                .build();
        Account receiver = Account.builder()
                .iban("RECEIVER")
                .balance(BigDecimal.ZERO)
                .build();
        when(accountRepository.findByIban("SENDER")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIban("RECEIVER")).thenReturn(Optional.of(receiver));
        TransferCommand request = command("20.00");

        BankOperationException exception = assertThrows(BankOperationException.class,
                () -> service.execute(request, TransferPurpose.NORMAL));

        assertEquals("Yetersiz bakiye!", exception.getMessage());
        assertEquals(new BigDecimal("10.00"), sender.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository, currencyService, rabbitPublisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"50000.00", "50000.01", "60000.00"})
    void normalTransferAtOrAboveThresholdWaitsForApproval(String amount) {
        Account sender = account("SENDER", "100000.00", Account.Currency.TRY);
        Account receiver = account("RECEIVER", "100.00", Account.Currency.TRY);
        stubAccounts(sender, receiver);
        when(currencyService.convertAmount(Double.valueOf(amount), "TRY", "TRY"))
                .thenReturn(Double.valueOf(amount));

        var result = service.execute(command(amount), TransferPurpose.NORMAL);

        assertEquals(Transaction.TransactionStatus.PENDING_APPROVAL, result.getStatus());
        assertEquals(new BigDecimal("100000.00").subtract(new BigDecimal(amount)), sender.getBalance());
        assertEquals(new BigDecimal("100.00"), receiver.getBalance());
        verify(accountRepository, never()).save(receiver);
        var message = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(rabbitPublisher).sendNotification(message.capture());
        assertEquals(NotificationMessage.NotificationType.SYSTEM_ALERT, message.getValue().getNotificationType());
    }

    @Test
    void normalTransferBelowThresholdCompletes() {
        Account sender = account("SENDER", "100000.00", Account.Currency.TRY);
        Account receiver = account("RECEIVER", "100.00", Account.Currency.TRY);
        stubAccounts(sender, receiver);
        when(currencyService.convertAmount(49999.99, "TRY", "TRY")).thenReturn(49999.99);

        var result = service.execute(command("49999.99"), TransferPurpose.NORMAL);

        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(0, new BigDecimal("50099.99").compareTo(receiver.getBalance()));
        verify(accountRepository).save(sender);
        verify(accountRepository).save(receiver);
        assertTrue(result.getReferenceNo() != null && !result.getReferenceNo().isBlank());
    }

    @Test
    void salaryAboveThresholdCompletesAndPreservesHistoryType() {
        Account sender = account("SENDER", "100000.00", Account.Currency.TRY);
        Account receiver = account("RECEIVER", "100.00", Account.Currency.TRY);
        stubAccounts(sender, receiver);
        when(currencyService.convertAmount(60000.0, "TRY", "TRY")).thenReturn(60000.0);

        var result = service.execute(command("60000.00"), TransferPurpose.SALARY);

        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(Transaction.TransactionType.TRANSFER, result.getTransactionType());
        assertEquals(0, new BigDecimal("60100.00").compareTo(receiver.getBalance()));
        var message = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(rabbitPublisher).sendNotification(message.capture());
        assertEquals(NotificationMessage.NotificationType.EMAIL, message.getValue().getNotificationType());
    }

    @Test
    void foreignNormalTransferUsesTryEquivalentForApproval() {
        Account sender = account("SENDER", "2000.00", Account.Currency.USD);
        Account receiver = account("RECEIVER", "0.00", Account.Currency.EUR);
        stubAccounts(sender, receiver);
        when(currencyService.convertAmount(1000.0, "USD", "EUR")).thenReturn(900.0);
        when(currencyService.convertAmount(1000.0, "USD", "TRY")).thenReturn(60000.0);

        var result = service.execute(command("1000.00"), TransferPurpose.NORMAL);

        assertEquals(Transaction.TransactionStatus.PENDING_APPROVAL, result.getStatus());
        assertEquals(0, new BigDecimal("900.00").compareTo(result.getConvertedAmount()));
        assertEquals(0, receiver.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void salaryStillRequiresSenderOwnership() {
        Account sender = account("SENDER", "100000.00", Account.Currency.TRY);
        sender.setOwnerIdentityNumber("22222222222");
        stubAccounts(sender, account("RECEIVER", "0.00", Account.Currency.TRY));

        assertThrows(BankOperationException.class, () -> service.execute(command("60000.00"), TransferPurpose.SALARY));
        assertEquals(new BigDecimal("100000.00"), sender.getBalance());
        verifyNoInteractions(transactionRepository, currencyService, rabbitPublisher);
    }

    @Test
    void inactiveReceiverStopsSalaryWithoutSideEffects() {
        Account sender = account("SENDER", "100000.00", Account.Currency.TRY);
        Account receiver = account("RECEIVER", "0.00", Account.Currency.TRY);
        receiver.setActive(false);
        stubAccounts(sender, receiver);

        assertThrows(BankOperationException.class, () -> service.execute(command("60000.00"), TransferPurpose.SALARY));
        assertEquals(new BigDecimal("100000.00"), sender.getBalance());
        verifyNoInteractions(transactionRepository, currencyService, rabbitPublisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void nonPositiveAmountIsRejected(String amount) {
        Account sender = account("SENDER", "100000.00", Account.Currency.TRY);
        stubAccounts(sender, account("RECEIVER", "0.00", Account.Currency.TRY));
        assertThrows(BankOperationException.class, () -> service.execute(command(amount), TransferPurpose.NORMAL));
        verifyNoInteractions(transactionRepository, currencyService, rabbitPublisher);
    }

    private TransferCommand command(String amount) {
        return new TransferCommand("SENDER", "RECEIVER", new BigDecimal(amount), null, "11111111111");
    }

    private Account account(String iban, String balance, Account.Currency currency) {
        return Account.builder().iban(iban).balance(new BigDecimal(balance)).currency(currency)
                .ownerIdentityNumber("11111111111").build();
    }

    private void stubAccounts(Account sender, Account receiver) {
        when(accountRepository.findByIban("SENDER")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIban("RECEIVER")).thenReturn(Optional.of(receiver));
    }
}
