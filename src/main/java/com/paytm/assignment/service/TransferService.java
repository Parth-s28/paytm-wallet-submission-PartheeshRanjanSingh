package com.paytm.assignment.service;

import com.paytm.assignment.api.TransferModels;
import com.paytm.assignment.repository.TransferRepository;
import com.paytm.assignment.repository.WalletRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private final JdbcTemplate jdbc;
    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;

    private final Counter transfersCreatedCounter;
    private final Counter transfersDeclinedCounter;
    private final Counter transfersReplayCounter;

    public TransferService(JdbcTemplate jdbc,
                           TransferRepository transferRepository,
                           WalletRepository walletRepository,
                           MeterRegistry registry) {
        this.jdbc = jdbc;
        this.transferRepository = transferRepository;
        this.walletRepository = walletRepository;

        this.transfersCreatedCounter = registry.counter("transfers.created.total");
        this.transfersDeclinedCounter = registry.counter("transfers.declined.total");
        this.transfersReplayCounter = registry.counter("transfers.replays.total");
    }

    public TransferModels.TransferResponse handleTransfer(TransferModels.TransferRequest request) {
        String key = request.idempotencyKey();

        Optional<TransferModels.TransferResponse> existing = transferRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            transfersReplayCounter.increment();
            return existing.get();
        }

        try {
            return executeTransfer(request);
        } catch (DuplicateKeyException e) {
            transfersReplayCounter.increment();
            return transferRepository.findByIdempotencyKey(key)
                    .orElseThrow(() -> new IllegalStateException("Transfer record lost after conflict resolution"));
        }
    }

    @Transactional
    public TransferModels.TransferResponse executeTransfer(TransferModels.TransferRequest request) {
        String idempotencyKey = request.idempotencyKey();

        Optional<TransferModels.TransferResponse> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Validate payload fields before invoking logic to avoid NullPointerExceptions
        if (request.fromWalletId() == null || request.toWalletId() == null || request.amountPaise() == null) {
            throw new IllegalArgumentException("from_wallet_id, to_wallet_id, and amount_paise must not be null");
        }

        UUID fromWalletId = request.fromWalletId();
        UUID toWalletId = request.toWalletId();
        long amountPaise = request.amountPaise();

        if (fromWalletId.equals(toWalletId)) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "SAME_WALLET_TRANSFER");
        }

        UUID firstLock = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        UUID secondLock = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

        jdbc.queryForList("SELECT id FROM wallets WHERE id IN (?, ?) ORDER BY id FOR UPDATE", firstLock, secondLock);

        Long senderBalance = jdbc.queryForObject("SELECT balance_paise FROM wallets WHERE id = ?", Long.class, fromWalletId);
        if (senderBalance == null) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "SENDER_NOT_FOUND");
        }

        if (senderBalance < amountPaise) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "INSUFFICIENT_FUNDS");
        }

        jdbc.update("UPDATE wallets SET balance_paise = balance_paise - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", amountPaise, fromWalletId);
        jdbc.update("UPDATE wallets SET balance_paise = balance_paise + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", amountPaise, toWalletId);

        UUID transferId = UUID.randomUUID();
        transferRepository.create(transferId, idempotencyKey, fromWalletId, toWalletId, amountPaise, "SUCCESS", null);
        transfersCreatedCounter.increment();

        return transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    }

    private TransferModels.TransferResponse recordFailedTransfer(String idempotencyKey, UUID fromWalletId, UUID toWalletId, long amountPaise, String reason) {
        UUID transferId = UUID.randomUUID();
        transferRepository.create(transferId, idempotencyKey, fromWalletId, toWalletId, amountPaise, "DECLINED", reason);
        transfersDeclinedCounter.increment();
        return transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    }
}