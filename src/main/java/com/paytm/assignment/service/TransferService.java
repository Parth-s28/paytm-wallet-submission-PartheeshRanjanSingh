package com.paytm.assignment.service;

import com.paytm.assignment.api.TransferModels;
import com.paytm.assignment.repository.TransferRepository;
import com.paytm.assignment.repository.WalletRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private final JdbcTemplate jdbc;
    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final TransactionTemplate transactionTemplate;

    private final Counter transfersCreatedCounter;
    private final Counter transfersDeclinedCounter;
    private final Counter transfersReplayCounter;

    public TransferService(JdbcTemplate jdbc,
                           TransferRepository transferRepository,
                           WalletRepository walletRepository,
                           TransactionTemplate transactionTemplate,
                           MeterRegistry registry) {
        this.jdbc = jdbc;
        this.transferRepository = transferRepository;
        this.walletRepository = walletRepository;
        this.transactionTemplate = transactionTemplate; // Spring provides this automatically!

        this.transfersCreatedCounter = registry.counter("transfers.created.total");
        this.transfersDeclinedCounter = registry.counter("transfers.declined.total");
        this.transfersReplayCounter = registry.counter("transfers.replays.total");
    }

    public TransferModels.TransferResponse handleTransfer(TransferModels.TransferRequest request) {
        String key = request.getIdempotencyKey();

        Optional<TransferModels.TransferResponse> existing = transferRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            transfersReplayCounter.increment();
            return existing.get();
        }

        try {
            // Programmatically execute the logic inside a guaranteed transaction boundary
            return transactionTemplate.execute(status -> executeTransferInsideTransaction(request));
        } catch (DuplicateKeyException e) {
            transfersReplayCounter.increment();
            return transferRepository.findByIdempotencyKey(key)
                    .orElseThrow(() -> new IllegalStateException("Transfer record lost after conflict resolution"));
        }
    }

    // Notice this is now PRIVATE and has NO @Transactional annotation.
    // It runs safely because TransactionTemplate wraps it.
    private TransferModels.TransferResponse executeTransferInsideTransaction(TransferModels.TransferRequest request) {
        String idempotencyKey = request.getIdempotencyKey();

        Optional<TransferModels.TransferResponse> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (request.getFromWalletId() == null || request.getToWalletId() == null || request.getAmountPaise() == null) {
            throw new IllegalArgumentException("from_wallet_id, to_wallet_id, and amount_paise must not be null");
        }

        UUID fromWalletId = request.getFromWalletId();
        UUID toWalletId = request.getToWalletId();
        long amountPaise = request.getAmountPaise();

        if (fromWalletId.equals(toWalletId)) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "SAME_WALLET_TRANSFER");
        }

        // Strict Lexicographical Lock Ordering
        UUID firstLock = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        UUID secondLock = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

        jdbc.queryForList("SELECT id FROM wallets WHERE id = ? FOR UPDATE", firstLock);
        jdbc.queryForList("SELECT id FROM wallets WHERE id = ? FOR UPDATE", secondLock);

        List<Long> balances = jdbc.queryForList("SELECT balance_paise FROM wallets WHERE id = ?", Long.class, fromWalletId);
        if (balances.isEmpty() || balances.get(0) == null) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "SENDER_NOT_FOUND");
        }
        Long senderBalance = balances.get(0);

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