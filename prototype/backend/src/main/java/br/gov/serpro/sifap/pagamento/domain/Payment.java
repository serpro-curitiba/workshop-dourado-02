package br.gov.serpro.sifap.pagamento.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "beneficiary_cpf", nullable = false, length = 11)
    private String beneficiaryCpf;

    @Column(name = "social_program_code", nullable = false, length = 20)
    private String socialProgramCode;

    @Column(nullable = false, length = 7)
    private String competence; // YYYY-MM

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "nominal_date", nullable = false)
    private LocalDate nominalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Payment() {}

    public Payment(String beneficiaryCpf, String socialProgramCode, String competence,
                   BigDecimal grossAmount, BigDecimal netAmount, LocalDate nominalDate) {
        this.beneficiaryCpf = beneficiaryCpf;
        this.socialProgramCode = socialProgramCode;
        this.competence = competence;
        this.grossAmount = grossAmount;
        this.netAmount = netAmount;
        this.nominalDate = nominalDate;
        this.status = PaymentStatus.G;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getBeneficiaryCpf() { return beneficiaryCpf; }
    public String getSocialProgramCode() { return socialProgramCode; }
    public String getCompetence() { return competence; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public LocalDate getNominalDate() { return nominalDate; }
    public PaymentStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void markPending() { this.status = PaymentStatus.P; }
    public void markSent() { this.status = PaymentStatus.E; }
    public void markReconciled() { this.status = PaymentStatus.C; }
    public void markReturned() { this.status = PaymentStatus.R; }
}
