package com.mustafa.service;

import com.mustafa.dto.request.CompanySyncRequest;

public interface IInternalCompanyService {
    void syncNewCompany(CompanySyncRequest request);
}