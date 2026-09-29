package com.mustafa.service.transfer;

import java.math.BigDecimal;

/** Selected by the application workflow, never by the public request. */
public enum TransferPurpose {
    NORMAL,
    SALARY;

    private static final BigDecimal NORMAL_APPROVAL_THRESHOLD_TRY = new BigDecimal("50000");

    public boolean requiresApproval(BigDecimal amountInTry) {
        return this == NORMAL && amountInTry.compareTo(NORMAL_APPROVAL_THRESHOLD_TRY) >= 0;
    }
}
