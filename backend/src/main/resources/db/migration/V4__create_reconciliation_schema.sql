-- ============================================================
-- V4 — Reconciliation schema: reconciliation_matches, reconciliation_discrepancies
-- ============================================================

CREATE TABLE reconciliation_matches (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    bank_statement_id   UUID         NOT NULL REFERENCES bank_statements(id),
    receivable_id       UUID         NOT NULL REFERENCES receivables(id),
    match_type          VARCHAR(20)  NOT NULL,
    matched_by_user_id  UUID,
    difference_cents    BIGINT       NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reconciliation_matches UNIQUE (bank_statement_id, receivable_id),
    CONSTRAINT chk_reconciliation_matches_type
        CHECK (match_type IN ('EXACT', 'FUZZY', 'MANUAL'))
);

CREATE INDEX idx_reconciliation_matches_tenant
    ON reconciliation_matches(tenant_id);

CREATE INDEX idx_reconciliation_matches_tenant_created
    ON reconciliation_matches(tenant_id, created_at DESC);

CREATE TABLE reconciliation_discrepancies (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID         NOT NULL,
    type                 VARCHAR(30)  NOT NULL,
    bank_statement_id    UUID         REFERENCES bank_statements(id),
    receivable_id        UUID         REFERENCES receivables(id),
    amount_cents         BIGINT,
    description          TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    resolved_at          TIMESTAMPTZ,
    resolved_by_user_id  UUID,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_reconciliation_discrepancies_type
        CHECK (type IN ('FEE_DIVERGENCE', 'MISSING_CREDIT', 'UNEXPECTED_DEBIT', 'DATE_DIVERGENCE', 'CHARGEBACK')),
    CONSTRAINT chk_reconciliation_discrepancies_status
        CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED'))
);

CREATE INDEX idx_reconciliation_discrepancies_tenant_status
    ON reconciliation_discrepancies(tenant_id, status);

CREATE INDEX idx_reconciliation_discrepancies_tenant_created
    ON reconciliation_discrepancies(tenant_id, created_at DESC);