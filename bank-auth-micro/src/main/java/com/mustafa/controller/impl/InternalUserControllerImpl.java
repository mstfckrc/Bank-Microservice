package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalUserController;
import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.service.IInternalUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserControllerImpl implements IInternalUserController {

    private final IInternalUserService internalUserService;

    @Override
    public CustomerProfileResponse getCustomerProfile(@PathVariable("identityNumber") String identityNumber) {
        log.info("INTERNAL KAPISI: Yetkili servisten müşteri profili talebi geldi.");
        return internalUserService.getCustomerProfile(identityNumber);
    }
}
