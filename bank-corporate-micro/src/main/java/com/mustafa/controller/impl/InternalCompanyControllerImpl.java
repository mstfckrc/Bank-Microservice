package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalCompanyController;
import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.service.IInternalCompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/companies")
@RequiredArgsConstructor
public class InternalCompanyControllerImpl implements IInternalCompanyController {

    private final IInternalCompanyService internalCompanyService;

    @Override
    @PostMapping("/sync")
    public ResponseEntity<Void> syncNewCompany(@RequestBody CompanySyncRequest request) {
        log.info("CONTROLLER: Karargahtan senkronizasyon (KAYIT) emri alındı. TC/VKN: {}", request.getCompanyIdentityNumber());
        internalCompanyService.syncNewCompany(request);
        return ResponseEntity.ok().build();
    }

    @Override
    @PutMapping("/sync/{identityNumber}")
    public ResponseEntity<Void> updateCompanyInfo(
            @PathVariable String identityNumber,
            @RequestBody CompanySyncRequest request) {
        log.info("CONTROLLER: Karargahtan GÜNCELLEME emri alındı. TC/VKN: {}", identityNumber);
        internalCompanyService.updateCompanyInfo(identityNumber, request);
        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping("/sync/{identityNumber}")
    public ResponseEntity<Void> deleteCompanyInfo(@PathVariable String identityNumber) {
        log.info("CONTROLLER: Karargahtan SİLME emri alındı. TC/VKN: {}", identityNumber);
        internalCompanyService.deleteCompanyInfo(identityNumber);
        return ResponseEntity.ok().build();
    }
}