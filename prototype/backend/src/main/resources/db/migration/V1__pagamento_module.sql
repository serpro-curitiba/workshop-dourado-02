-- SIFAP 2.0 — schema inicial do bounded context PAGAMENTO
-- ADR-002: PostgreSQL 16 + Strangler Fig (escrita dual durante cutover)
-- source_legacy: 01-arqueologia/legado-sifap/adabas-ddms/PAGAMENTO.ddm

CREATE TABLE IF NOT EXISTS payment (
    id                  UUID PRIMARY KEY,
    beneficiary_cpf     VARCHAR(11)   NOT NULL,
    social_program_code VARCHAR(20)   NOT NULL,
    competence          VARCHAR(7)    NOT NULL,   -- YYYY-MM
    gross_amount        NUMERIC(15,2) NOT NULL,
    net_amount          NUMERIC(15,2) NOT NULL,
    nominal_date        DATE          NOT NULL,
    status              VARCHAR(1)    NOT NULL,   -- G,P,E,C,R
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_payment_status CHECK (status IN ('G','P','E','C','R'))
);

CREATE INDEX IF NOT EXISTS idx_payment_competence ON payment (competence);
CREATE INDEX IF NOT EXISTS idx_payment_cpf        ON payment (beneficiary_cpf);
CREATE INDEX IF NOT EXISTS idx_payment_status     ON payment (status);
