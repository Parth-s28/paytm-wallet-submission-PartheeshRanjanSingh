package com.paytm.assignment.service;

import com.paytm.assignment.api.WalletModels;
import com.paytm.assignment.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository repository;
    private final long initialBalancePaise;

    public WalletService(WalletRepository repository,
                         @Value("${wallet.initial-balance-paise}") long initialBalancePaise) {
        this.repository = repository;
        this.initialBalancePaise = initialBalancePaise;
    }

    public WalletModels.WalletResponse getOrCreate(String userId) {
        repository.insertIfAbsent(UUID.randomUUID(), userId, initialBalancePaise);
        log.info("domain_event=wallet_accessed message=\"Wallet accessed or created for user: {}\"", userId);
        return repository.findByUserId(userId);
    }

    public WalletModels.WalletResponse get(String id) {
        return repository.findById(UUID.fromString(id));
    }
}