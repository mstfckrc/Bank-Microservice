package com.mustafa.dto.response;

// 🚀 DÜZELTİLDİ: Artık Entity'den değil, kendi bağımsız enum paketimizden geliyor!
import com.mustafa.dto.enums.TransactionType;
import com.mustafa.dto.enums.TransactionStatus;
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

    private String referenceNo; // Dekont Numarası
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String description;
    private LocalDateTime transactionDate;
    private Long receiverAccountId;
    private Long senderAccountId;
    private BigDecimal convertedAmount;
}