package com.paytm.assignment.api;
import java.util.UUID;
public final class WalletModels {
    private WalletModels() {}
    public record WalletResponse(UUID id, String userId, long balancePaise) {}
}
