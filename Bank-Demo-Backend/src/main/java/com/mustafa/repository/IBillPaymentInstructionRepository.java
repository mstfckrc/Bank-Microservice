package com.mustafa.repository;

import com.mustafa.entity.BillPaymentInstruction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBillPaymentInstructionRepository extends JpaRepository<BillPaymentInstruction, Long> {

    // 🚀 Günü gelen ve aktif olan talimatları, bağlı oldukları Hesap ve Müşteriyle birlikte tek seferde (JOIN FETCH) çeker.
    @EntityGraph(attributePaths = {"account", "account.appUser"})
    List<BillPaymentInstruction> findByIsActiveTrueAndPaymentDay(Integer paymentDay);

    // Müşterinin (AppUser) talimatlarını web sitesinde göstermek için
    List<BillPaymentInstruction> findByAccount_AppUser_IdentityNumber(String identityNumber);
}