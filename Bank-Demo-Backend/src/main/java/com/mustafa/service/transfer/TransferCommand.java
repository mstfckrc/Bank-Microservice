package com.mustafa.service.transfer;

import java.math.BigDecimal;

/** Internal money movement data, independent of the HTTP request model. */
public record TransferCommand(String senderIban, String receiverIban, BigDecimal amount,
                              String description, String ownerIdentityNumber) {
}
