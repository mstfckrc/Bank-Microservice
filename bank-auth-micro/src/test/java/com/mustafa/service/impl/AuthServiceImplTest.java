package com.mustafa.service.impl;

import com.mustafa.client.CompanyServiceClient;
import com.mustafa.dto.request.RegisterRequest;
import com.mustafa.exception.BankOperationException;
import com.mustafa.messaging.publisher.RabbitMQPublisher;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.ICaptchaService;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final IAppUserRepository appUserRepository = mock(IAppUserRepository.class);
    private final IRetailCustomerRepository retailCustomerRepository = mock(IRetailCustomerRepository.class);
    private final RabbitMQPublisher rabbitPublisher = mock(RabbitMQPublisher.class);
    private final CompanyServiceClient companyServiceClient = mock(CompanyServiceClient.class);
    private final Keycloak keycloak = mock(Keycloak.class);
    private final ICaptchaService captchaService = mock(ICaptchaService.class);

    private final AuthServiceImpl service = new AuthServiceImpl(
            appUserRepository, retailCustomerRepository, rabbitPublisher,
            companyServiceClient, keycloak, captchaService);

    @Test
    void failedCaptchaStopsRegistrationBeforeExternalCalls() {
        RegisterRequest request = new RegisterRequest();
        request.setIdentityNumber("11111111111");
        request.setCaptchaToken("invalid");
        when(captchaService.verifyToken("invalid")).thenReturn(false);

        BankOperationException exception = assertThrows(BankOperationException.class,
                () -> service.register(request));

        assertEquals("Güvenlik doğrulaması başarısız! Lütfen robot olmadığınızı kanıtlayıp tekrar deneyin.",
                exception.getMessage());
        verify(captchaService).verifyToken("invalid");
        verifyNoInteractions(appUserRepository, retailCustomerRepository, rabbitPublisher,
                companyServiceClient, keycloak);
    }
}
