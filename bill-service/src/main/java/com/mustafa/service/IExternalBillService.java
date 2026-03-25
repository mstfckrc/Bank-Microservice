package com.mustafa.service;

import com.mustafa.entity.BillPaymentInstruction;
import java.math.BigDecimal;

public interface IExternalBillService {
    BigDecimal getDebt(String subscriberNo, BillPaymentInstruction.BillType billType);
}