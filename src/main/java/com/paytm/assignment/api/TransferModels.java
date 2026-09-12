package com.paytm.assignment.api;

import java.time.Instant;
import java.util.UUID;

public class TransferModels {

    public static class TransferRequest {
        private String idempotencyKey;
        private UUID fromWalletId;
        private UUID toWalletId;
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

        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        public void setFromWalletId(UUID fromWalletId) { this.fromWalletId = fromWalletId; }
        public void setToWalletId(UUID toWalletId) { this.toWalletId = toWalletId; }
        public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }
    }

    public static class TransferResponse {
        private UUID id;
        private String idempotencyKey;
        private UUID fromWalletId;
        private UUID toWalletId;
        private Long amountPaise;
        private String status;
        private String declineReason;
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

        public void setId(UUID id) { this.id = id; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        public void setFromWalletId(UUID fromWalletId) { this.fromWalletId = fromWalletId; }
        public void setToWalletId(UUID toWalletId) { this.toWalletId = toWalletId; }
        public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }
        public void setStatus(String status) { this.status = status; }
        public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}