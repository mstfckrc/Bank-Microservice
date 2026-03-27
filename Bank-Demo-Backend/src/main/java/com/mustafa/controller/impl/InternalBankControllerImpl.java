package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalBankController;
import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.CustomerProfileResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.service.IInternalBankService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalBankControllerImpl implements IInternalBankController {

    private final IInternalBankService internalBankService;
    private final HttpServletRequest request; // 🚀 YENİ EKLENDİ: İstek başlıklarını okumak için

    @Override
    public ResponseEntity<AccountValidationResponse> validateAccount(String iban) {
        log.info("INTERNAL REST İsteği: Kasa (IBAN) doğrulama talebi geldi. IBAN: {}", iban);
        return ResponseEntity.ok(internalBankService.validateAccount(iban));
    }

    @Override
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(String identityNumber) {
        log.info("INTERNAL REST İsteği: Müşteri profili sorgulanıyor. TC: {}", identityNumber);
        return ResponseEntity.ok(internalBankService.getCustomerProfile(identityNumber));
    }

    @Override
    public ResponseEntity<List<TransactionResponse>> payBulkSalaries(BulkSalaryRequest bulkRequest) {

        // 🚀 MİKROSERVİS KİMLİK KÖPRÜSÜ: Kurumsal servisten gelen TC'yi oku
        String internalIdentity = request.getHeader("X-Internal-Identity");

        if (internalIdentity != null) {
            log.info("INTERNAL GÜVENLİK: İç hattan gelen işlem, SecurityContext'e TC işleniyor: {}", internalIdentity);
            // Karargahın güvenlik duvarına (SecurityContext) bu TC'yi yetkili olarak manuel ekle
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    internalIdentity, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CORPORATE_MANAGER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            log.warn("🚨 İÇ HAT UYARISI: X-Internal-Identity başlığı gelmedi, işlem anonim olarak devam edecek!");
        }

        log.info("INTERNAL REST İsteği: Toplu maaş dağıtım emri alındı! Çıkış Kasası: {}", bulkRequest.getSenderIban());
        return ResponseEntity.ok(internalBankService.payBulkSalaries(bulkRequest));
    }
}