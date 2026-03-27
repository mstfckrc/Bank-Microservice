package com.mustafa.controller;

import com.mustafa.dto.request.CompanySyncRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/internal/companies")
public interface IInternalCompanyController {

    @PostMapping("/sync")
    ResponseEntity<Void> syncNewCompany(@RequestBody CompanySyncRequest request);
}