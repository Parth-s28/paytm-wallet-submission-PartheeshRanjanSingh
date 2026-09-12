package com.paytm.assignment.repository;
import com.paytm.assignment.api.WalletModels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public class WalletRepository {
    private final JdbcTemplate jdbc;
    public WalletRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insertIfAbsent(UUID id, String userId, long initialBalancePaise) {
        jdbc.update("""
            INSERT INTO wallets(id, user_id, balance_paise)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id) DO NOTHING
            """, id, userId, initialBalancePaise);
    }

    public WalletModels.WalletResponse findByUserId(String userId) {
        return jdbc.queryForObject("""
            SELECT id, user_id, balance_paise FROM wallets WHERE user_id = ?
            """, (rs, n) -> new WalletModels.WalletResponse(
                rs.getObject("id", UUID.class), rs.getString("user_id"), rs.getLong("balance_paise")), userId);
    }

    public WalletModels.WalletResponse findById(UUID id) {
        return jdbc.queryForObject("""
            SELECT id, user_id, balance_paise FROM wallets WHERE id = ?
            """, (rs, n) -> new WalletModels.WalletResponse(
                rs.getObject("id", UUID.class), rs.getString("user_id"), rs.getLong("balance_paise")), id);
    }
}
