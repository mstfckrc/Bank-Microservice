package com.mustafa.service.application;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.controller.impl.InternalBankControllerImpl;
import com.mustafa.dto.request.SalaryPaymentItem;
import com.mustafa.entity.Account;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.IInternalBankService;
import com.mustafa.service.transfer.MoneyTransferService;
import com.mustafa.service.transfer.TransferCommand;
import com.mustafa.service.transfer.TransferPurpose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayrollApplicationServiceTest {
    private final IAccountRepository accounts = mock(IAccountRepository.class);
    private final ICurrencyService currency = mock(ICurrencyService.class);
    private final MoneyTransferService transfers = mock(MoneyTransferService.class);
    private final PayrollApplicationService service = new PayrollApplicationService(accounts, currency, transfers);

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
    void existingBulkSalaryHttpContractStillSelectsSalaryWorkflow() throws Exception {
        stubSender(Account.Currency.TRY, "200000.00");
        var servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("X-Internal-Identity", "11111111111");
        var mvc = MockMvcBuilders.standaloneSetup(new InternalBankControllerImpl(
                mock(IInternalBankService.class), service, servletRequest)).build();

        mvc.perform(post("/api/v1/internal/transactions/bulk-salary").contentType("application/json")
                .content("""
                        {"senderIban":"SENDER","companyName":"Demo","salaryItems":[
                          {"receiverIban":"EMPLOYEE1","amount":60000},
                          {"receiverIban":"EMPLOYEE2","amount":70000}]}
                        """)).andExpect(status().isOk());

        verify(transfers, times(2)).execute(any(TransferCommand.class), eq(TransferPurpose.SALARY));
        verify(transfers, never()).execute(any(TransferCommand.class), eq(TransferPurpose.NORMAL));
    }

    @Test
    void batchMapsEachEmployeeToSalaryWorkflow() {
        stubSender(Account.Currency.TRY, "200000.00");
        var results = service.payBulkSalaries(request("60000.00", "70000.00"));

        assertEquals(2, results.size());
        verify(transfers).execute(new TransferCommand("SENDER", "EMPLOYEE1", new BigDecimal("60000.00"),
                "Maaş Ödemesi - Demo", "11111111111"), TransferPurpose.SALARY);
        verify(transfers).execute(new TransferCommand("SENDER", "EMPLOYEE2", new BigDecimal("70000.00"),
                "Maaş Ödemesi - Demo", "11111111111"), TransferPurpose.SALARY);
        verifyNoInteractions(currency);
    }

    @Test
    void foreignCompanyAccountPaysTrySalariesUsingSourceCurrency() {
        stubSender(Account.Currency.USD, "10000.00");
        when(currency.convertAmount(130000.0, "TRY", "USD")).thenReturn(3250.0);
        when(currency.convertAmount(60000.0, "TRY", "USD")).thenReturn(1500.0);
        when(currency.convertAmount(70000.0, "TRY", "USD")).thenReturn(1750.0);

        service.payBulkSalaries(request("60000.00", "70000.00"));

        verify(transfers).execute(new TransferCommand("SENDER", "EMPLOYEE1", BigDecimal.valueOf(1500.0),
                "Maaş Ödemesi - Demo (Orijinal: 60000.00 TRY)", "11111111111"), TransferPurpose.SALARY);
        verify(transfers).execute(new TransferCommand("SENDER", "EMPLOYEE2", BigDecimal.valueOf(1750.0),
                "Maaş Ödemesi - Demo (Orijinal: 70000.00 TRY)", "11111111111"), TransferPurpose.SALARY);
    }

    @Test
    void insufficientBatchBalanceDoesNotStartAnyEmployeePayment() {
        stubSender(Account.Currency.TRY, "100000.00");
        assertThrows(BankOperationException.class, () -> service.payBulkSalaries(request("60000.00", "70000.00")));
        verifyNoInteractions(transfers);
    }

    @Test
    void closedCompanyAccountDoesNotStartAnyPayment() {
        Account sender = stubSender(Account.Currency.TRY, "200000.00");
        sender.setActive(false);
        assertThrows(BankOperationException.class, () -> service.payBulkSalaries(request("60000.00", "70000.00")));
        verifyNoInteractions(currency, transfers);
    }

    private Account stubSender(Account.Currency currencyType, String balance) {
        Account sender = Account.builder().iban("SENDER").currency(currencyType)
                .balance(new BigDecimal(balance)).ownerIdentityNumber("11111111111").build();
        when(accounts.findByIban("SENDER")).thenReturn(Optional.of(sender));
        return sender;
    }

    private BulkSalaryRequest request(String first, String second) {
        return new BulkSalaryRequest("SENDER", "Demo", List.of(
                new SalaryPaymentItem("EMPLOYEE1", new BigDecimal(first)),
                new SalaryPaymentItem("EMPLOYEE2", new BigDecimal(second))));
    }
}
