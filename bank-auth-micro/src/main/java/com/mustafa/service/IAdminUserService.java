package com.mustafa.service;

import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import java.util.List;

public interface IAdminUserService {
    List<UserProfileResponse> getAllCustomers();
    UserProfileResponse updateCustomer(String identityNumber, UpdateProfileRequest request);
    void updateCustomerStatus(String identityNumber, String status);
    void deleteCustomer(String identityNumber);
}