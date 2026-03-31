package com.mustafa.controller;

import com.mustafa.dto.request.ChangePasswordRequest;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public interface ICustomerController {
    ResponseEntity<UserProfileResponse> getMyProfile();
    ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request);
    ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request);
    ResponseEntity<Map<String, String>> appealRejection();
}