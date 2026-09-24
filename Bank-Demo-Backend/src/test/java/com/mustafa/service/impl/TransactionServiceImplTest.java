package com.mustafa.service.impl;

import com.mustafa.dto.request.TransferRequest;
import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.ICurrencyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionServiceImplTest {

    private final IAccountRepository accountRepository = mock(IAccountRepository.class);
    private final ITransactionRepository transactionRepository = mock(ITransactionRepository.class);
    private final ICurrencyService currencyService = mock(ICurrencyService.class);
    private final RabbitMQPublisher rabbitPublisher = mock(RabbitMQPublisher.class);

    private final TransactionServiceImpl service = new TransactionServiceImpl(
            accountRepository, transactionRepository, currencyService, rabbitPublisher);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insufficientFundsStopsTransferBeforeAnySideEffect() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("11111111111", null, List.of()));
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
        TransferRequest request = TransferRequest.builder()
                .senderIban("SENDER")
                .receiverIban("RECEIVER")
                .amount(new BigDecimal("20.00"))
                .build();

        BankOperationException exception = assertThrows(BankOperationException.class,
                () -> service.transfer(request));

        assertEquals("Yetersiz bakiye!", exception.getMessage());
        assertEquals(new BigDecimal("10.00"), sender.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository, currencyService, rabbitPublisher);
    }
}
