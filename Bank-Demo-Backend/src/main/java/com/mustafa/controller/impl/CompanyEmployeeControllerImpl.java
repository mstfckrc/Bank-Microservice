package com.mustafa.controller.impl;

import com.mustafa.controller.ICompanyEmployeeController;
import com.mustafa.dto.request.AutoPaymentSettingsRequest;
import com.mustafa.dto.request.HireEmployeeRequest;
import com.mustafa.dto.request.SalaryPaymentRequest;
import com.mustafa.dto.request.UpdateEmployeeRequest;
import com.mustafa.dto.response.AutoPaymentSettingsResponse;
import com.mustafa.dto.response.CompanyEmployeeResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.service.ICompanyEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j // 🚀 LOGGER AKTİF
@RestController
@RequestMapping("/api/v1/companies/employees")
@RequiredArgsConstructor
public class CompanyEmployeeControllerImpl implements ICompanyEmployeeController {

    private final ICompanyEmployeeService companyEmployeeService;

    @Override
    @PostMapping
    public ResponseEntity<CompanyEmployeeResponse> hireEmployee(
            Principal principal,
            @RequestBody HireEmployeeRequest request) {

        log.info("REST İsteği: Yeni personel işe alım talebi alındı.");
        String managerIdentityNumber = principal.getName();
        return new ResponseEntity<>(companyEmployeeService.hireEmployee(managerIdentityNumber, request), HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<CompanyEmployeeResponse>> getMyEmployees(Principal principal) {
        log.info("REST İsteği: Kurumsal müşterinin personel listesi sorgulanıyor.");
        String managerIdentityNumber = principal.getName();
        return ResponseEntity.ok(companyEmployeeService.getMyEmployees(managerIdentityNumber));
    }

    @Override
    @PutMapping("/{employeeIdentityNumber}")
    public ResponseEntity<CompanyEmployeeResponse> updateEmployee(
            Principal principal,
            @PathVariable String employeeIdentityNumber,
            @RequestBody UpdateEmployeeRequest request) {

        log.info("REST İsteği: Personel maaş/IBAN bilgileri güncelleniyor.");
        String managerIdentityNumber = principal.getName();
        return ResponseEntity.ok(companyEmployeeService.updateEmployee(managerIdentityNumber, employeeIdentityNumber, request));
    }

    @Override
    @DeleteMapping("/{employeeIdentityNumber}")
    public ResponseEntity<Map<String, String>> removeEmployee(
            Principal principal,
            @PathVariable String employeeIdentityNumber) {

        log.info("REST İsteği: Personeli işten çıkarma talebi alındı.");
        String managerIdentityNumber = principal.getName();
        companyEmployeeService.removeEmployee(managerIdentityNumber, employeeIdentityNumber);

        Map<String, String> response = new HashMap<>();
        response.put("message", employeeIdentityNumber + " kimlik numaralı personel başarıyla şirketten çıkarıldı.");
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/pay-salaries")
    public ResponseEntity<List<TransactionResponse>> paySalaries(
            Principal principal,
            @RequestBody SalaryPaymentRequest request) {

        log.info("REST İsteği: Toplu maaş dağıtım operasyonu tetiklendi.");
        String managerIdentityNumber = principal.getName();
        return ResponseEntity.ok(companyEmployeeService.paySalaries(managerIdentityNumber, request));
    }

    // 🚀 YENİ EKLENEN KAPI (ENDPOINT)
    @Override
    @PutMapping("/auto-payment-settings")
    public ResponseEntity<AutoPaymentSettingsResponse> updateAutoPaymentSettings(
            Principal principal,
            @Valid @RequestBody AutoPaymentSettingsRequest request) {

        log.info("REST İsteği: Şirket otomatik maaş ödeme ayarlarını güncelleme talebi alındı.");
        String managerIdentityNumber = principal.getName();
        return ResponseEntity.ok(companyEmployeeService.updateAutoPaymentSettings(managerIdentityNumber, request));
    }

    // 🚀 YENİ EKLENEN KAPI: Ayarları okumak için
    @Override
    @GetMapping("/auto-payment-settings")
    public ResponseEntity<AutoPaymentSettingsResponse> getAutoPaymentSettings(Principal principal) {
        log.info("REST İsteği: Şirket otomatik maaş ödeme ayarları getiriliyor.");
        String managerIdentityNumber = principal.getName();
        return ResponseEntity.ok(companyEmployeeService.getAutoPaymentSettings(managerIdentityNumber));
    }
}