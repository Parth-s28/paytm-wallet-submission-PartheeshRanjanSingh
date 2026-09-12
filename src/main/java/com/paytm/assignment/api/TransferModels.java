package com.paytm.assignment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class TransferModels {

    public static class TransferRequest {
        @JsonProperty("idempotency_key")
        private String idempotencyKey;

        @JsonProperty("from_wallet_id")
        private UUID fromWalletId;

        @JsonProperty("to_wallet_id")
        private UUID toWalletId;

        @JsonProperty("amount_paise")
        private Long amountPaise;

        public TransferRequest() {}

        public TransferRequest(String idempotencyKey, UUID fromWalletId, UUID toWalletId, Long amountPaise) {
            this.idempotencyKey = idempotencyKey;
            this.fromWalletId = fromWalletId;
            this.toWalletId = toWalletId;
            this.amountPaise = amountPaise;
        }

        public String getIdempotencyKey() { return idempotencyKey; }
        public UUID getFromWalletId() { return fromWalletId; }
        public UUID getToWalletId() { return toWalletId; }
        public Long getAmountPaise() { return amountPaise; }
    }

    public static class TransferResponse {
        @JsonProperty("id")
        private UUID id;

        @JsonProperty("idempotency_key")
        private String idempotencyKey;

        @JsonProperty("from_wallet_id")
        private UUID fromWalletId;

        @JsonProperty("to_wallet_id")
        private UUID toWalletId;

        @JsonProperty("amount_paise")
        private Long amountPaise;

        @JsonProperty("status")
        private String status;

        @JsonProperty("decline_reason")
        private String declineReason;

        @JsonProperty("created_at")
        private Instant createdAt;

        public TransferResponse() {}

        public TransferResponse(UUID id, String idempotencyKey, UUID fromWalletId, UUID toWalletId, Long amountPaise, String status, String declineReason, Instant createdAt) {
            this.id = id;
            this.idempotencyKey = idempotencyKey;
            this.fromWalletId = fromWalletId;
            this.toWalletId = toWalletId;
            this.amountPaise = amountPaise;
            this.status = status;
            this.declineReason = declineReason;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public UUID getFromWalletId() { return fromWalletId; }
        public UUID getToWalletId() { return toWalletId; }
        public Long getAmountPaise() { return amountPaise; }
        public String getStatus() { return status; }
        public String getDeclineReason() { return declineReason; }
        public Instant getCreatedAt() { return createdAt; }
    }
}