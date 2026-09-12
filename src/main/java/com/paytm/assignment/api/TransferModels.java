package com.paytm.assignment.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

public class TransferModels {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TransferRequest(
            String idempotencyKey,
            UUID fromWalletId,
            UUID toWalletId,
            Long amountPaise
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TransferResponse(
            UUID id,
            String idempotencyKey,
            UUID fromWalletId,
            UUID toWalletId,
            Long amountPaise,
            String status,
            String declineReason,
            Instant createdAt
    ) {}
}