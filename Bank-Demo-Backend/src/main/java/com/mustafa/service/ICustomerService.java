package com.mustafa.service;

import com.mustafa.dto.request.ChangePasswordRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;

public interface ICustomerService {
    UserProfileResponse updateProfile(UpdateProfileRequest request);
    void changePassword(ChangePasswordRequest request);
    UserProfileResponse getMyProfile();
    void appealRejection();
}