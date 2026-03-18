package com.mustafa.service;

import com.mustafa.entity.BillPaymentInstruction;
import java.math.BigDecimal;

public interface IExternalBillService {
    // Dış kurumdan abone numarasına göre borç sorgulayan metot
    BigDecimal getDebt(String subscriberNo, BillPaymentInstruction.BillType billType);
}