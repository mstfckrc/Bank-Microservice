package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalCompanyController;
import com.mustafa.dto.request.CompanySyncRequest;
import com.mustafa.service.IInternalCompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalCompanyControllerImpl implements IInternalCompanyController {

    private final IInternalCompanyService internalCompanyService;

    @Override
    public ResponseEntity<Void> syncNewCompany(CompanySyncRequest request) {
        // 🚀 DÜZELTİLDİ: getManagerIdentityNumber yerine getCompanyIdentityNumber kullanıldı!
        log.info("CONTROLLER: Karargahtan senkronizasyon emri alındı. TC/VKN: {}", request.getCompanyIdentityNumber());
        internalCompanyService.syncNewCompany(request);
        return ResponseEntity.ok().build();
    }
}