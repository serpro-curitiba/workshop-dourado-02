package br.gov.serpro.sifap.conciliacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_divergence")
public class ReconciliationDivergence {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 7)
    private String competence;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false)
    private boolean conflict;

    @Column(nullable = false, length = 1000)
    private String detail;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    protected ReconciliationDivergence() {
    }

    public ReconciliationDivergence(String competence, String type, boolean conflict, String detail) {
        this.id = UUID.randomUUID();
        this.competence = competence;
        this.type = type;
        this.conflict = conflict;
        this.detail = detail;
        this.detectedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getCompetence() { return competence; }
    public String getType() { return type; }
    public boolean isConflict() { return conflict; }
    public String getDetail() { return detail; }
    public OffsetDateTime getDetectedAt() { return detectedAt; }
}