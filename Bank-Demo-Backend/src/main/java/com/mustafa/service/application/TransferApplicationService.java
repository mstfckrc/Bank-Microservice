package com.mustafa.service.application;

import com.mustafa.dto.request.TransferRequest;
import com.mustafa.dto.response.TransactionResponse;
import com.mustafa.service.transfer.MoneyTransferService;
import com.mustafa.service.transfer.TransferCommand;
import com.mustafa.service.transfer.TransferPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferApplicationService {
    private final MoneyTransferService moneyTransferService;

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String identity = SecurityContextHolder.getContext().getAuthentication().getName();
        TransferCommand command = new TransferCommand(request.getSenderIban(), request.getReceiverIban(),
                request.getAmount(), request.getDescription(), identity);
        return moneyTransferService.execute(command, TransferPurpose.NORMAL);
    }
}
