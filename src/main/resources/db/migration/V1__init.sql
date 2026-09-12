CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    balance_paise BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wallet_balance_non_negative CHECK (balance_paise >= 0)
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    from_wallet_id UUID NOT NULL REFERENCES wallets(id),
    to_wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    status VARCHAR(30) NOT NULL,
    decline_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT different_wallets CHECK (from_wallet_id <> to_wallet_id)
);
