package com.mustafa.service.impl;

import com.mustafa.client.BackendServiceClient;
import com.mustafa.entity.BillPaymentInstruction;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IBillPaymentInstructionRepository;
import com.mustafa.service.IExternalBillService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BillPaymentServiceTest {

    private final IBillPaymentInstructionRepository instructionRepository = mock(IBillPaymentInstructionRepository.class);
    private final IExternalBillService externalBillService = mock(IExternalBillService.class);
    private final BackendServiceClient backendServiceClient = mock(BackendServiceClient.class);
    private final RabbitMQPublisher rabbitPublisher = mock(RabbitMQPublisher.class);

    private final BillPaymentServiceImpl service = new BillPaymentServiceImpl(
            instructionRepository, externalBillService, backendServiceClient, rabbitPublisher);

    @Test
    void cannotDeleteAnotherCustomersInstruction() {
        BillPaymentInstruction instruction = BillPaymentInstruction.builder()
                .identityNumber("11111111111")
                .build();
        when(instructionRepository.findById(42L)).thenReturn(Optional.of(instruction));

        BankOperationException exception = assertThrows(BankOperationException.class,
                () -> service.deleteInstruction("22222222222", 42L));

        assertEquals("Yetkisiz işlem! Bu talimat size ait değil.", exception.getMessage());
        verify(instructionRepository, never()).delete(any());
        verifyNoInteractions(externalBillService, backendServiceClient, rabbitPublisher);
    }
}
