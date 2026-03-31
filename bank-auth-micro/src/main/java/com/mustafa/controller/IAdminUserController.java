package com.mustafa.controller;

import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface IAdminUserController {
    ResponseEntity<List<UserProfileResponse>> getAllCustomers();
    ResponseEntity<UserProfileResponse> updateCustomer(String identityNumber, UpdateProfileRequest request);
    ResponseEntity<Void> updateCustomerStatus(String identityNumber, String status);
    ResponseEntity<Void> deleteCustomer(String identityNumber);
}