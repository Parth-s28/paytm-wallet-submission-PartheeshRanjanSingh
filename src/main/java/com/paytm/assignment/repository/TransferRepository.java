package com.paytm.assignment.repository;

import com.paytm.assignment.api.TransferModels;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransferRepository {

    private final JdbcTemplate jdbc;

    public TransferRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID id, String idempotencyKey, UUID fromWalletId, UUID toWalletId, long amountPaise, String status, String declineReason) {
        jdbc.update("""
            INSERT INTO transfers (id, idempotency_key, from_wallet_id, to_wallet_id, amount_paise, status, decline_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, id, idempotencyKey, fromWalletId, toWalletId, amountPaise, status, declineReason);
    }

    public Optional<TransferModels.TransferResponse> findByIdempotencyKey(String idempotencyKey) {
        try {
            TransferModels.TransferResponse transfer = jdbc.queryForObject("""
                SELECT id, idempotency_key, from_wallet_id, to_wallet_id, amount_paise, status, decline_reason, created_at
                FROM transfers WHERE idempotency_key = ?
                """, this::mapRowToTransferResponse, idempotencyKey);
            return Optional.ofNullable(transfer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private TransferModels.TransferResponse mapRowToTransferResponse(ResultSet rs, int rowNum) throws SQLException {
        return new TransferModels.TransferResponse(
                rs.getObject("id", UUID.class),
                rs.getString("idempotency_key"),
                rs.getObject("from_wallet_id", UUID.class),
                rs.getObject("to_wallet_id", UUID.class),
                rs.getLong("amount_paise"),
                rs.getString("status"),
                rs.getString("decline_reason"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}