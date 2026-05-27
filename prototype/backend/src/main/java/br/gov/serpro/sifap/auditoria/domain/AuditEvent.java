package br.gov.serpro.sifap.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "prev_state", length = 1)
    private String prevState;

    @Column(name = "new_state", length = 1)
    private String newState;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false, length = 4000)
    private String payload;

    protected AuditEvent() {
    }

    public AuditEvent(String actor, String action, UUID paymentId, String prevState, String newState, String payload) {
        this.id = UUID.randomUUID();
        this.actor = actor;
        this.action = action;
        this.paymentId = paymentId;
        this.prevState = prevState;
        this.newState = newState;
        this.payload = payload == null || payload.isBlank() ? "{}" : payload;
        this.occurredAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public UUID getPaymentId() { return paymentId; }
    public String getPrevState() { return prevState; }
    public String getNewState() { return newState; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public String getPayload() { return payload; }
}