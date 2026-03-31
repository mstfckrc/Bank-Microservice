package com.mustafa.controller.impl;

import com.mustafa.controller.IInternalBankController;
import com.mustafa.dto.request.BulkSalaryRequest;
import com.mustafa.dto.response.AccountValidationResponse;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.service.IInternalBankService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal") // 🚀 Sınıf seviyesine taşındı
@RequiredArgsConstructor
public class InternalBankControllerImpl implements IInternalBankController {

    private final IInternalBankService internalBankService;
    private final HttpServletRequest request;

    @Override
    @GetMapping("/accounts/validate") // 🚀 Metot seviyesine taşındı
    public ResponseEntity<AccountValidationResponse> validateAccount(@RequestParam("iban") String iban) {
        log.info("INTERNAL REST İsteği: Kasa (IBAN) doğrulama talebi geldi. IBAN: {}", iban);
        return ResponseEntity.ok(internalBankService.validateAccount(iban));
    }

    @Override
    @PostMapping("/transactions/bulk-salary") // 🚀 Metot seviyesine taşındı
    public ResponseEntity<List<TransactionResponse>> payBulkSalaries(@RequestBody BulkSalaryRequest bulkRequest) {

        String internalIdentity = request.getHeader("X-Internal-Identity");

        if (internalIdentity != null) {
            log.info("INTERNAL GÜVENLİK: İç hattan gelen işlem, SecurityContext'e TC işleniyor: {}", internalIdentity);
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

    @Override
    @DeleteMapping("/accounts/customer/{identityNumber}") // 🚀 Metot seviyesine taşındı
    public ResponseEntity<Void> deleteCustomerAccounts(@PathVariable("identityNumber") String identityNumber) {
        log.info("INTERNAL REST İsteği: Kimlik Üssünden hesap iptal emri geldi. TC: {}", identityNumber);
        internalBankService.deleteCustomerAccounts(identityNumber);
        return ResponseEntity.ok().build();
    }
}