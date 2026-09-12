package com.paytm.assignment.api;

import java.time.Instant;
import java.util.UUID;

public class TransferModels {

    public record TransferRequest(
            UUID fromWalletId,
            UUID toWalletId,
            long amountPaise
    ) {}

    public record TransferResponse(
            UUID id,
            String idempotencyKey,
            UUID fromWalletId,
            UUID toWalletId,
            long amountPaise,
            String status,
            String declineReason,
            Instant createdAt
    ) {}
}