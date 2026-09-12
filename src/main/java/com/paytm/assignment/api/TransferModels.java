package com.paytm.assignment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class TransferModels {

    public record TransferRequest(
            @JsonProperty("idempotency_key") String idempotencyKey,
            @JsonProperty("from_wallet_id") UUID fromWalletId,
            @JsonProperty("to_wallet_id") UUID toWalletId,
            @JsonProperty("amount_paise") Long amountPaise
    ) {}

    public record TransferResponse(
            @JsonProperty("id") UUID id,
            @JsonProperty("idempotency_key") String idempotencyKey,
            @JsonProperty("from_wallet_id") UUID fromWalletId,
            @JsonProperty("to_wallet_id") UUID toWalletId,
            @JsonProperty("amount_paise") Long amountPaise,
            @JsonProperty("status") String status,
            @JsonProperty("decline_reason") String declineReason,
            @JsonProperty("created_at") Instant createdAt
    ) {}
}