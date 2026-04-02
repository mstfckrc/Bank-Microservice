package com.mustafa.controller;

import com.mustafa.dto.response.CustomerProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface IInternalUserController {

    @GetMapping("/profile/{identityNumber}")
    CustomerProfileResponse getCustomerProfile(@PathVariable("identityNumber") String identityNumber);

}