package com.mustafa.controller;

import com.mustafa.dto.request.AutoPaymentSettingsRequest;
import com.mustafa.dto.request.HireEmployeeRequest;
import com.mustafa.dto.request.SalaryPaymentRequest;
import com.mustafa.dto.request.UpdateEmployeeRequest;
import com.mustafa.dto.response.AutoPaymentSettingsResponse;
import com.mustafa.dto.response.CompanyEmployeeResponse;
import com.mustafa.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface ICompanyEmployeeController {

    ResponseEntity<CompanyEmployeeResponse> hireEmployee(Principal principal, @Valid @RequestBody HireEmployeeRequest request);

    ResponseEntity<List<CompanyEmployeeResponse>> getMyEmployees(Principal principal);

    ResponseEntity<CompanyEmployeeResponse> updateEmployee(
            Principal principal,
            @PathVariable String employeeIdentityNumber,
            @Valid @RequestBody UpdateEmployeeRequest request);

    ResponseEntity<Map<String, String>> removeEmployee(
            Principal principal,
            @PathVariable String employeeIdentityNumber);

    ResponseEntity<List<TransactionResponse>> paySalaries(
            Principal principal,
            @Valid @RequestBody SalaryPaymentRequest request);

    // 🚀 YENİ EKLENEN KAPI İMZASI
    ResponseEntity<AutoPaymentSettingsResponse> updateAutoPaymentSettings(
            Principal principal,
            @Valid @RequestBody AutoPaymentSettingsRequest request);

    ResponseEntity<AutoPaymentSettingsResponse> getAutoPaymentSettings(Principal principal);
}