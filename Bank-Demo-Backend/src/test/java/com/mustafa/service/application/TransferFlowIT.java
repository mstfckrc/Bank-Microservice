package com.mustafa.service.application;

import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.request.SalaryPaymentItem;
import com.mustafa.dto.request.TransferRequest;
import com.mustafa.entity.Account;
import com.mustafa.entity.Transaction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAccountRepository;
import com.mustafa.repository.ITransactionRepository;
import com.mustafa.service.ICurrencyService;
import com.mustafa.service.transfer.MoneyTransferService;
import com.mustafa.service.transfer.TransferCommand;
import com.mustafa.service.transfer.TransferPurpose;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Explicitly run against a disposable local DB, never the application's DB_URL. */
@EnabledIfSystemProperty(named = "test.db.url",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/bankdemo_transfer_test")
@SpringJUnitConfig(TransferFlowIT.Config.class)
class TransferFlowIT {
    @Autowired IAccountRepository accounts;
    @Autowired ITransactionRepository transactions;
    @Autowired TransferApplicationService transfers;
    @Autowired PayrollApplicationService payroll;
    @Autowired MoneyTransferService moneyTransferService;

    @BeforeEach
    void prepare() {
        transactions.deleteAll();
        accounts.deleteAll();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("11111111111", null, List.of()));
        account("SENDER", "0000000001", "200000.00", true);
        account("EMPLOYEE1", "0000000002", "100.00", true);
        account("EMPLOYEE2", "0000000003", "200.00", true);
        account("EMPLOYEE3", "0000000004", "300.00", false);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulPayrollCommitsAllEmployeePaymentsTogether() {
        var result = payroll.payBulkSalaries(batch(false));
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == Transaction.TransactionStatus.COMPLETED));
        assertBalance("SENDER", "70000.00");
        assertBalance("EMPLOYEE1", "60100.00");
        assertBalance("EMPLOYEE2", "70200.00");
        assertEquals(2, transactions.count());
    }

    @Test
    void thirdEmployeeFailureRollsBackFirstTwoPaymentsAndLedgerEntries() {
        assertThrows(BankOperationException.class, () -> payroll.payBulkSalaries(batch(true)));
        assertBalance("SENDER", "200000.00");
        assertBalance("EMPLOYEE1", "100.00");
        assertBalance("EMPLOYEE2", "200.00");
        assertBalance("EMPLOYEE3", "300.00");
        assertEquals(0, transactions.count());
    }

    @Test
    void normalHighValueTransferCommitsPendingWithoutCreditingReceiver() {
        var result = transfers.transfer(TransferRequest.builder().senderIban("SENDER")
                .receiverIban("EMPLOYEE1").amount(new BigDecimal("60000.00")).build());
        assertEquals(Transaction.TransactionStatus.PENDING_APPROVAL, result.getStatus());
        assertBalance("SENDER", "140000.00");
        assertBalance("EMPLOYEE1", "100.00");
        assertEquals(Transaction.TransactionStatus.PENDING_APPROVAL,
                transactions.findByReferenceNo(result.getReferenceNo()).orElseThrow().getStatus());
    }

    @Test
    void sharedEngineCannotRunOutsideAnApplicationTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> moneyTransferService.execute(
                new TransferCommand("SENDER", "EMPLOYEE1", new BigDecimal("10.00"), null, "11111111111"),
                TransferPurpose.NORMAL));
        assertEquals(0, transactions.count());
    }

    private BulkSalaryRequest batch(boolean includeThird) {
        var items = new java.util.ArrayList<>(List.of(
                new SalaryPaymentItem("EMPLOYEE1", new BigDecimal("60000.00")),
                new SalaryPaymentItem("EMPLOYEE2", new BigDecimal("70000.00"))));
        if (includeThird) items.add(new SalaryPaymentItem("EMPLOYEE3", new BigDecimal("10000.00")));
        return new BulkSalaryRequest("SENDER", "Demo", items);
    }

    private void account(String iban, String number, String balance, boolean active) {
        accounts.save(Account.builder().iban(iban).accountNumber(number).balance(new BigDecimal(balance))
                .ownerIdentityNumber("11111111111").currency(Account.Currency.TRY).isActive(active).build());
    }

    private void assertBalance(String iban, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(accounts.findByIban(iban).orElseThrow().getBalance()));
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = IAccountRepository.class)
    @Import({TransferApplicationService.class, PayrollApplicationService.class, MoneyTransferService.class})
    static class Config {
        @Bean
        DataSource dataSource() {
            String url = System.getProperty("test.db.url", "");
            if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/bankdemo_transfer_test")) {
                throw new IllegalArgumentException("Only an explicitly configured disposable local test DB is allowed");
            }
            return new DriverManagerDataSource(url, "bankdemo_test", "isolated-test-only");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.mustafa.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        ICurrencyService currencyService() {
            var currency = mock(ICurrencyService.class);
            when(currency.convertAmount(anyDouble(), anyString(), anyString())).thenAnswer(invocation -> {
                if (!invocation.getArgument(1).equals(invocation.getArgument(2))) {
                    throw new IllegalArgumentException("This integration fixture uses TRY accounts only");
                }
                return invocation.getArgument(0);
            });
            return currency;
        }

        @Bean
        RabbitMQPublisher rabbitPublisher() {
            return mock(RabbitMQPublisher.class);
        }
    }
}
