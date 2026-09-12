package com.paytm.assignment.service;

import com.paytm.assignment.api.TransferModels;
import com.paytm.assignment.repository.TransferRepository;
import com.paytm.assignment.repository.WalletRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private final JdbcTemplate jdbc;
    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;

    public TransferService(JdbcTemplate jdbc,
                           TransferRepository transferRepository,
                           WalletRepository walletRepository) {
        this.jdbc = jdbc;
        this.transferRepository = transferRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public TransferModels.TransferResponse transferMoney(String idempotencyKey, UUID fromWalletId, UUID toWalletId, long amountPaise) {
        // 1. Idempotency Check: Return previous response if key exists
        Optional<TransferModels.TransferResponse> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Prevent self-transfer (schema constraint backup)
        if (fromWalletId.equals(toWalletId)) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "SAME_WALLET_TRANSFER");
        }

        // 3. Lock wallets in deterministic order by UUID to prevent deadlocks
        UUID firstLock = fromWalletId.compareTo(toWalletId) < 0 ? fromWalletId : toWalletId;
        UUID secondLock = fromWalletId.compareTo(toWalletId) < 0 ? toWalletId : fromWalletId;

        // Execute explicit pessimistic row locks
        jdbc.queryForList("SELECT id FROM wallets WHERE id IN (?, ?) ORDER BY id FOR UPDATE", firstLock, secondLock);

        // 4. Fetch updated sender balance
        Long senderBalance = jdbc.queryForObject("SELECT balance_paise FROM wallets WHERE id = ?", Long.class, fromWalletId);
        if (senderBalance == null) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "SENDER_NOT_FOUND");
        }

        // 5. Insufficient funds check
        if (senderBalance < amountPaise) {
            return recordFailedTransfer(idempotencyKey, fromWalletId, toWalletId, amountPaise, "INSUFFICIENT_FUNDS");
        }

        // 6. Deduct from sender & Credit to receiver
        jdbc.update("UPDATE wallets SET balance_paise = balance_paise - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", amountPaise, fromWalletId);
        jdbc.update("UPDATE wallets SET balance_paise = balance_paise + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", amountPaise, toWalletId);

        // 7. Record successful transfer audit row
        UUID transferId = UUID.randomUUID();
        try {
            transferRepository.create(transferId, idempotencyKey, fromWalletId, toWalletId, amountPaise, "SUCCESS", null);
        } catch (DuplicateKeyException e) {
            // Concurrent request with same idempotency key won race condition
            return transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }

        return transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    }

    private TransferModels.TransferResponse recordFailedTransfer(String idempotencyKey, UUID fromWalletId, UUID toWalletId, long amountPaise, String reason) {
        UUID transferId = UUID.randomUUID();
        try {
            transferRepository.create(transferId, idempotencyKey, fromWalletId, toWalletId, amountPaise, "DECLINED", reason);
        } catch (DuplicateKeyException e) {
            return transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }
        return transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
    }
}