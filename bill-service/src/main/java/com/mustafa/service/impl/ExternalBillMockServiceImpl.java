package com.mustafa.service.impl;

import com.mustafa.entity.BillPaymentInstruction;
import com.mustafa.service.IExternalBillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class ExternalBillMockServiceImpl implements IExternalBillService {

    @Override
    public BigDecimal getDebt(String subscriberNo, BillPaymentInstruction.BillType billType) {
        // Kurumlardan gelen rastgele borç tutarı
        double randomDebt = 100.0 + (Math.random() * 900.0);
        BigDecimal debt = BigDecimal.valueOf(randomDebt).setScale(2, RoundingMode.HALF_UP);

        log.info("📡 DIŞ SİSTEM: {} kurumundan Abone {} için borç sorgulandı. Tutar: {} TRY", billType, subscriberNo, debt);
        return debt;
    }
}