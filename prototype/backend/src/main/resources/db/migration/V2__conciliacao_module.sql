-- V2__conciliacao_module.sql
-- Bounded context: conciliacao
-- REQ-PAY-008 / BR-022 (GREENFIELD): idempotencia de importacao CNAB 240 via SHA-256.
-- Resolve loop operacional do legado BATCHCON (reprocessamento permitido).

CREATE TABLE bank_return_file (
    id           UUID PRIMARY KEY,
    sha256       VARCHAR(64)  NOT NULL UNIQUE,
    filename     VARCHAR(255) NOT NULL,
    size_bytes   BIGINT       NOT NULL CHECK (size_bytes > 0),
    imported_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_bank_return_file_imported_at ON bank_return_file (imported_at);
