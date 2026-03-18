package com.mustafa.dto.response;

import com.mustafa.entity.Transaction.TransactionType;
import com.mustafa.entity.Transaction.TransactionStatus; // 🚀 YENİ EKLENDİ
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private String referenceNo; // Dekont Numarası (Örn: TXN-58294A)
    private BigDecimal amount;
    private TransactionType transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER

    private TransactionStatus status; // 🚀 YENİ EKLENEN (İşlem Onay Durumu)

    private String description;
    private LocalDateTime transactionDate;
    private Long receiverAccountId;
    private Long senderAccountId;
    private BigDecimal convertedAmount;
}