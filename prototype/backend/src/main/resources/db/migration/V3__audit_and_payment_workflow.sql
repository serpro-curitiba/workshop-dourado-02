-- REQ-PAY-006/009/010/014: approval workflow, audit trail and secure payment responses.
-- source_legacy: 01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm

ALTER TABLE payment ADD COLUMN IF NOT EXISTS bank_code VARCHAR(3) NOT NULL DEFAULT '001';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS agency VARCHAR(5) NOT NULL DEFAULT '0001';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS account VARCHAR(12) NOT NULL DEFAULT '000000000000';

CREATE TABLE IF NOT EXISTS audit_event (
    id          UUID PRIMARY KEY,
    actor       VARCHAR(120) NOT NULL,
    action      VARCHAR(64)  NOT NULL,
    payment_id  UUID NULL,
    prev_state  VARCHAR(1) NULL,
    new_state   VARCHAR(1) NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload     VARCHAR(4000) NOT NULL DEFAULT '{}',
    CONSTRAINT fk_audit_payment FOREIGN KEY (payment_id) REFERENCES payment(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_event_occurred_at ON audit_event (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_payment ON audit_event (payment_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_action ON audit_event (action);