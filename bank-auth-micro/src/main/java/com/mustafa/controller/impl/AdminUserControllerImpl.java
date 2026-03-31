package com.mustafa.controller.impl;

import com.mustafa.controller.IAdminUserController;
import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.dto.response.UserProfileResponse;
import com.mustafa.service.IAdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminUserControllerImpl implements IAdminUserController {

    private final IAdminUserService adminUserService;

    @Override
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllCustomers() {
        log.info("REST İsteği: Admin tarafından tüm müşteriler sorgulanıyor.");
        return ResponseEntity.ok(adminUserService.getAllCustomers());
    }

    @Override
    @PutMapping("/{identityNumber}")
    public ResponseEntity<UserProfileResponse> updateCustomer(
            @PathVariable String identityNumber,
            @RequestBody UpdateProfileRequest request) {
        log.info("REST İsteği: Admin müşteri ({}) profilini güncelliyor.", identityNumber);
        return ResponseEntity.ok(adminUserService.updateCustomer(identityNumber, request));
    }

    @Override
    @PutMapping("/{identityNumber}/status")
    public ResponseEntity<Void> updateCustomerStatus(
            @PathVariable String identityNumber,
            @RequestParam String status) {
        log.info("REST İsteği: Admin müşterinin ({}) statüsünü {} yapıyor.", identityNumber, status);
        adminUserService.updateCustomerStatus(identityNumber, status);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping("/{identityNumber}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String identityNumber) {
        log.info("REST İsteği: Admin müşteri ({}) silme işlemini başlattı.", identityNumber);
        adminUserService.deleteCustomer(identityNumber);
        return ResponseEntity.ok().build();
    }
}