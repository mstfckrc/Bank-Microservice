package com.mustafa.service.application;

import com.mustafa.controller.impl.TransactionControllerImpl;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.entity.Transaction;
import com.mustafa.service.ITransactionService;
import com.mustafa.service.transfer.MoneyTransferService;
import com.mustafa.service.transfer.TransferCommand;
import com.mustafa.service.transfer.TransferPurpose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferApplicationServiceTest {
    private final MoneyTransferService moneyTransferService = mock(MoneyTransferService.class);
    private final TransferApplicationService service = new TransferApplicationService(moneyTransferService);

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("11111111111", null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void normalWorkflowMapsRequestAndAlwaysSelectsNormalPolicy() {
        var request = TransferRequest.builder().senderIban("SENDER").receiverIban("RECEIVER")
                .amount(new BigDecimal("60000.00")).description("Kira").build();
        var command = new TransferCommand("SENDER", "RECEIVER", new BigDecimal("60000.00"), "Kira", "11111111111");
        var response = TransactionResponse.builder().status(Transaction.TransactionStatus.PENDING_APPROVAL).build();
        when(moneyTransferService.execute(command, TransferPurpose.NORMAL)).thenReturn(response);

        assertSame(response, service.transfer(request));
        verify(moneyTransferService).execute(command, TransferPurpose.NORMAL);
    }

    @Test
    void existingTransferHttpContractStillWorks() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(
                new TransactionControllerImpl(mock(ITransactionService.class), service)).build();
        mvc.perform(post("/api/v1/transactions/transfer").contentType("application/json").content("""
                {"senderIban":"SENDER","receiverIban":"RECEIVER","amount":60000,"description":"Kira"}
                """)).andExpect(status().isOk());
        verify(moneyTransferService).execute(any(TransferCommand.class), eq(TransferPurpose.NORMAL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"salaryPayment", "isSalaryPayment"})
    void injectedSalaryFlagCannotSelectSalaryWorkflow(String field) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(
                new TransactionControllerImpl(mock(ITransactionService.class), service)).build();
        var response = mvc.perform(post("/api/v1/transactions/transfer").contentType("application/json")
                .content("{\"senderIban\":\"SENDER\",\"receiverIban\":\"RECEIVER\",\"amount\":60000,\""
                        + field + "\":true}")).andReturn().getResponse();

        // Both rejecting unknown fields and ignoring them are safe; neither may select SALARY.
        assertTrue(response.getStatus() == 200 || response.getStatus() == 400);
        if (response.getStatus() == 200) {
            verify(moneyTransferService).execute(any(TransferCommand.class), eq(TransferPurpose.NORMAL));
        } else {
            verifyNoInteractions(moneyTransferService);
        }
        verify(moneyTransferService, never()).execute(any(TransferCommand.class), eq(TransferPurpose.SALARY));
    }
}
