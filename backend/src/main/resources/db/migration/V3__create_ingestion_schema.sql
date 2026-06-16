-- ============================================================
-- V3 — Ingestion schema: file_imports, bank_statements, receivables
-- ============================================================

CREATE TABLE file_imports (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID         NOT NULL,
    user_id           UUID         NOT NULL,
    file_name         VARCHAR(500) NOT NULL,
    file_type         VARCHAR(30)  NOT NULL,
    storage_key       TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_records     INT,
    processed_records INT,
    error_message     TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_file_imports_type   CHECK (file_type IN ('CNAB_240','CNAB_400','OFX','CSV_ADQUIRENTE')),
    CONSTRAINT chk_file_imports_status CHECK (status IN ('PENDING','PROCESSING','DONE','ERROR'))
);

CREATE INDEX idx_file_imports_tenant_status
    ON file_imports(tenant_id, status);

CREATE TABLE bank_statements (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID         NOT NULL,
    file_import_id        UUID         NOT NULL REFERENCES file_imports(id),
    bank_code             VARCHAR(10),
    agency                VARCHAR(20),
    account_number        VARCHAR(30),
    transaction_date      DATE         NOT NULL,
    value_date            DATE         NOT NULL,
    amount_cents          BIGINT       NOT NULL,
    description           VARCHAR(500),
    document_number       VARCHAR(50),
    reconciliation_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_bank_statements_recon_status
        CHECK (reconciliation_status IN ('PENDING','MATCHED','PARTIAL','UNMATCHED','ANTICIPATED'))
);

CREATE INDEX idx_bank_statements_tenant_status
    ON bank_statements(tenant_id, reconciliation_status);

CREATE INDEX idx_bank_statements_file_import_id
    ON bank_statements(file_import_id);

CREATE TABLE receivables (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID        NOT NULL,
    file_import_id          UUID        REFERENCES file_imports(id),
    source                  VARCHAR(30) NOT NULL,
    sale_date               DATE        NOT NULL,
    expected_settlement_date DATE       NOT NULL,
    gross_amount_cents      BIGINT      NOT NULL,
    fee_amount_cents        BIGINT      NOT NULL DEFAULT 0,
    net_amount_cents        BIGINT      NOT NULL,
    installment_number      INT         NOT NULL DEFAULT 1,
    total_installments      INT         NOT NULL DEFAULT 1,
    authorization_code      VARCHAR(50),
    nsu                     VARCHAR(50),
    reconciliation_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_receivables_source
        CHECK (source IN ('MANUAL','CIELO','REDE','STONE','PAGSEGURO','MERCADOPAGO','PIX','BOLETO','OTHER')),
    CONSTRAINT chk_receivables_recon_status
        CHECK (reconciliation_status IN ('PENDING','MATCHED','PARTIAL','UNMATCHED','ANTICIPATED'))
);

CREATE INDEX idx_receivables_tenant_status_date
    ON receivables(tenant_id, reconciliation_status, expected_settlement_date);