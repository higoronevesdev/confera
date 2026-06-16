-- ============================================================
-- V2 — Ledger schema: accounts, transactions, entries, outbox
-- ============================================================

-- Chart of accounts (imutável após criação)
CREATE TABLE accounts (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL,
    name           VARCHAR(255) NOT NULL,
    code           VARCHAR(20)  NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    normal_balance VARCHAR(10)  NOT NULL,
    parent_id      UUID         REFERENCES accounts(id),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounts_tenant_code    UNIQUE (tenant_id, code),
    CONSTRAINT chk_accounts_type          CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    CONSTRAINT chk_accounts_normal_balance CHECK (normal_balance IN ('DEBIT','CREDIT'))
);

-- Cabeçalho da transação
CREATE TABLE transactions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL,
    description     VARCHAR(500) NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    metadata        JSONB,
    idempotency_key VARCHAR(255)
);

CREATE UNIQUE INDEX idx_transactions_idempotency_key
    ON transactions(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_transactions_tenant_occurred_at
    ON transactions(tenant_id, occurred_at DESC);

-- Partidas do razão (imutável após criação)
CREATE TABLE entries (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID        NOT NULL REFERENCES transactions(id),
    account_id     UUID        NOT NULL REFERENCES accounts(id),
    tenant_id      UUID        NOT NULL,
    amount_cents   BIGINT      NOT NULL,
    direction      VARCHAR(10) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_entries_amount_cents CHECK (amount_cents > 0),
    CONSTRAINT chk_entries_direction    CHECK (direction IN ('DEBIT','CREDIT'))
);

CREATE INDEX idx_entries_account_created_at
    ON entries(account_id, created_at DESC);

CREATE INDEX idx_entries_transaction_id
    ON entries(transaction_id);

-- Outbox pattern: eventos pendentes de publicação no RabbitMQ
CREATE TABLE outbox_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    routing_key    VARCHAR(255),
    payload        JSONB        NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts       INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events(created_at)
    WHERE published = FALSE;