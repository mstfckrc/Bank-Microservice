package com.mustafa.repository;

import com.mustafa.entity.BillPaymentInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBillPaymentInstructionRepository extends JpaRepository<BillPaymentInstruction, Long> {

    // 🚀 MİKROSERVİS GÜNCELLEMESİ: Artık Account'a gitmiyoruz, doğrudan kimlik numarasıyla buluyoruz!
    List<BillPaymentInstruction> findByIdentityNumber(String identityNumber);

    // 🚀 YENİ: Otomasyon motoru için günü gelen aktif faturaları getir
    List<BillPaymentInstruction> findByIsActiveTrueAndPaymentDay(Integer paymentDay);
}