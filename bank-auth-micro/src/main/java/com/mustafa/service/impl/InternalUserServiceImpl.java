package com.mustafa.service.impl;

import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.entity.RetailCustomer;
import com.mustafa.exception.BankOperationException;
import com.mustafa.repository.IRetailCustomerRepository;
import com.mustafa.service.IInternalUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserServiceImpl implements IInternalUserService {

    private final IRetailCustomerRepository retailCustomerRepository;

    @Override
    public CustomerProfileResponse getCustomerProfile(String identityNumber) {
        log.info("INTERNAL SERVICE: {} TC kimlik numaralı müşterinin profil bilgisi veritabanından çekiliyor.", identityNumber);

        RetailCustomer customer = retailCustomerRepository.findByAppUser_IdentityNumber(identityNumber)
                .orElseThrow(() -> {
                    log.warn("İç İstihbarat Hatası: {} TC numaralı müşteri bulunamadı!", identityNumber);
                    return new BankOperationException("Müşteri sistemde bulunamadı!");
                });

        return CustomerProfileResponse.builder()
                .identityNumber(identityNumber)
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .build();
    }
}