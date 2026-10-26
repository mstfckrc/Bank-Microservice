package com.mustafa.controller;

import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.CustomerAccessContextResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface IInternalUserController {

    @GetMapping("/profile/{identityNumber}")
    CustomerProfileResponse getCustomerProfile(@PathVariable("identityNumber") String identityNumber);

    @GetMapping("/access-context/{identityNumber}")
    CustomerAccessContextResponse getCustomerAccessContext(@PathVariable("identityNumber") String identityNumber);

}
