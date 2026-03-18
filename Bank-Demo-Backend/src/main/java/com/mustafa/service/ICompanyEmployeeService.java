package com.mustafa.service;

import com.mustafa.dto.request.HireEmployeeRequest;
import com.mustafa.dto.request.UpdateEmployeeRequest;
import com.mustafa.dto.response.AutoPaymentSettingsResponse;
import com.mustafa.dto.response.CompanyEmployeeResponse;
import com.mustafa.dto.response.TransactionResponse;

import java.util.List;

public interface ICompanyEmployeeService {

    CompanyEmployeeResponse hireEmployee(String managerIdentityNumber, HireEmployeeRequest request);

    List<CompanyEmployeeResponse> getMyEmployees(String managerIdentityNumber);

    CompanyEmployeeResponse updateEmployee(String managerIdentityNumber, String employeeIdentityNumber, UpdateEmployeeRequest request);

    void removeEmployee(String managerIdentityNumber, String employeeIdentityNumber);

    // Mevcut manuel ödeme metodu (Kullanıcı için)
    List<TransactionResponse> paySalaries(String managerIdentityNumber, com.mustafa.dto.request.SalaryPaymentRequest request);

    // 🚀 YENİ: Otomatik sistemin güvenlik duvarına takılmadan kullanacağı metot
    List<TransactionResponse> paySalariesAutomatically(Long companyId, String senderIban);

    AutoPaymentSettingsResponse updateAutoPaymentSettings(String managerIdentityNumber, com.mustafa.dto.request.AutoPaymentSettingsRequest request);

    // Mevcut otomatik ödeme ayarlarını getir
    AutoPaymentSettingsResponse getAutoPaymentSettings(String managerIdentityNumber);
}