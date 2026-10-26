package com.mustafa.service.impl;

import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.CustomerAccessContextResponse;
import com.mustafa.entity.AppUser;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IAppUserRepository;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.IInternalUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserServiceImpl implements IInternalUserService {

    private final IRetailCustomerRepository retailCustomerRepository;
    private final IAppUserRepository appUserRepository;

    @Override
    public CustomerProfileResponse getCustomerProfile(String identityNumber) {
        log.info("INTERNAL SERVICE: Yetkili müşteri profili sorgusu işleniyor.");

        RetailCustomer customer = retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("İç profil sorgusunda müşteri bulunamadı.");
                    return new BankOperationException("Müşteri sistemde bulunamadı!");
                });

        return CustomerProfileResponse.builder()
                .identityNumber(identityNumber)
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .build();
    }

    @Override
    public CustomerAccessContextResponse getCustomerAccessContext(String identityNumber) {
        AppUser appUser = appUserRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı erişim kaydı bulunamadı."));

        return CustomerAccessContextResponse.builder()
                .role(appUser.getRole().name())
                .approvalStatus(appUser.getStatus().name())
                .accessRestrictionStatus(appUser.getAccessRestrictionStatus().name())
                .build();
    }
}
