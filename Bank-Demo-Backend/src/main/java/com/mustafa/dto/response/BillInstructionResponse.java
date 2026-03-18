package com.mustafa.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class BillInstructionResponse {
    private Long id;
    private String accountNumber;
    private String iban;
    private String billType;
    private String subscriberNo;
    private Integer paymentDay;
    private LocalDate lastPaymentDate;
    private boolean isActive;
}