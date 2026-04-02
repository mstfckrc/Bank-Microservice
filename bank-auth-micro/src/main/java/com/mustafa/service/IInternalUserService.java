package com.mustafa.service;

import com.mustafa.dto.response.CustomerProfileResponse;

public interface IInternalUserService {
    CustomerProfileResponse getCustomerProfile(String identityNumber);
}