-- REQ-PAY-007: conciliacao CNAB 240 com divergencias rastreaveis.
-- source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHCON.NSN#L55-L140

CREATE TABLE IF NOT EXISTS reconciliation_divergence (
    id          UUID PRIMARY KEY,
    competence  VARCHAR(7) NOT NULL,
    type        VARCHAR(40) NOT NULL,
    conflict    BOOLEAN NOT NULL,
    detail      VARCHAR(1000) NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_divergence_competence ON reconciliation_divergence (competence);
CREATE INDEX IF NOT EXISTS idx_reconciliation_divergence_type ON reconciliation_divergence (type);