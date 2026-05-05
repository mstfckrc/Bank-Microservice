package com.mustafa.service;

import com.mustafa.dto.request.CompanySyncRequest;

public interface IInternalCompanyService {

    void syncNewCompany(CompanySyncRequest request);

    void updateCompanyInfo(String identityNumber, CompanySyncRequest request);

    void deleteCompanyInfo(String identityNumber);

    CompanySyncRequest getCompanyInfo(String identityNumber);
}