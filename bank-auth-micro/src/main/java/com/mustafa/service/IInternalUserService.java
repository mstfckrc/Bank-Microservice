package com.mustafa.service;

import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.CustomerAccessContextResponse;

public interface IInternalUserService {
    CustomerProfileResponse getCustomerProfile(String identityNumber);
    CustomerAccessContextResponse getCustomerAccessContext(String identityNumber);
}
