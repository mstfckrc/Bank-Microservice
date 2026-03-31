package com.mustafa.controller;

import com.mustafa.dto.request.CompanySyncRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IInternalCompanyController {

    ResponseEntity<Void> syncNewCompany(@RequestBody CompanySyncRequest request);

    ResponseEntity<Void> updateCompanyInfo(@PathVariable String identityNumber, @RequestBody CompanySyncRequest request);

    ResponseEntity<Void> deleteCompanyInfo(@PathVariable String identityNumber);
}