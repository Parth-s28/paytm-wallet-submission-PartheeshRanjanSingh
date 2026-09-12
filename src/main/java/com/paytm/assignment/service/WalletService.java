package com.paytm.assignment.service;
import com.paytm.assignment.api.WalletModels;
import com.paytm.assignment.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class WalletService {
    private final WalletRepository repository;
    private final long initialBalancePaise;
    public WalletService(WalletRepository repository,
                         @Value("${wallet.initial-balance-paise}") long initialBalancePaise) {
        this.repository = repository; this.initialBalancePaise = initialBalancePaise;
    }
    public WalletModels.WalletResponse getOrCreate(String userId) {
        repository.insertIfAbsent(UUID.randomUUID(), userId, initialBalancePaise);
        return repository.findByUserId(userId);
    }
    public WalletModels.WalletResponse get(String id) {
        return repository.findById(UUID.fromString(id));
    }
}
