package com.mustafa.service;

import com.mustafa.entity.enums.BillType;

import java.math.BigDecimal;

public interface IExternalBillService {
    BigDecimal getDebt(String subscriberNo, BillType billType);
}