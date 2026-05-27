package br.gov.serpro.sifap.pagamento.application;

import br.gov.serpro.sifap.auditoria.application.AuditEventService;
import br.gov.serpro.sifap.calculo.application.BenefitCalculator;
import br.gov.serpro.sifap.calculo.application.JudicialDiscountService;
import br.gov.serpro.sifap.calculo.domain.CalculationContext;
import br.gov.serpro.sifap.calculo.domain.JudicialDiscountResult;
import br.gov.serpro.sifap.elegibilidade.application.EligibilityService;
import br.gov.serpro.sifap.elegibilidade.domain.EligibilityDecision;
import br.gov.serpro.sifap.pagamento.domain.Payment;
import br.gov.serpro.sifap.pagamento.domain.PaymentRepository;
import br.gov.serpro.sifap.pagamento.domain.PaymentStatus;
import br.gov.serpro.sifap.shared.security.DevActor;
import br.gov.serpro.sifap.shared.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Orquestracao dos pagamentos mensais.
 *
 * Implementa:
 *   REQ-PAY-001 (BR-007): so abre pagamentos no 5o dia util.
 *   REQ-PAY-011 (BR-010): regime de dezembro.
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN
 */
@Service
public class PaymentCycleService {

    private final PaymentRepository paymentRepository;
    private final SeedBeneficiaryProvider beneficiaryProvider;
    private final EligibilityService eligibilityService;
    private final BenefitCalculator benefitCalculator;
    private final JudicialDiscountService judicialDiscountService;
    private final AuditEventService auditEventService;
    private final Cnab240GeneratorService cnabGeneratorService;

    public PaymentCycleService(PaymentRepository paymentRepository,
                               SeedBeneficiaryProvider beneficiaryProvider,
                               EligibilityService eligibilityService,
                               BenefitCalculator benefitCalculator,
                               JudicialDiscountService judicialDiscountService,
                               AuditEventService auditEventService,
                               Cnab240GeneratorService cnabGeneratorService) {
        this.paymentRepository = paymentRepository;
        this.beneficiaryProvider = beneficiaryProvider;
        this.eligibilityService = eligibilityService;
        this.benefitCalculator = benefitCalculator;
        this.judicialDiscountService = judicialDiscountService;
        this.auditEventService = auditEventService;
        this.cnabGeneratorService = cnabGeneratorService;
    }

    @Transactional
    public CycleSummary openCycle(YearMonth competence, LocalDate today, DevActor actor) {
        String competenceKey = competence.toString(); // YYYY-MM
        if (paymentRepository.existsByCompetence(competenceKey)) {
            throw new CycleAlreadyExistsException(competenceKey);
        }
        LocalDate earliest = CycleCalendar.fifthBusinessDay(competence.getYear(), competence.getMonthValue());
        if (today.isBefore(earliest)) {
            throw new CycleTooEarlyException(competenceKey, earliest, today);
        }
        List<Payment> generated = beneficiaryProvider.eligibleBeneficiaries().stream()
                .map(beneficiary -> generatePayment(competenceKey, competence, beneficiary, actor))
                .toList();
        return new CycleSummary(competenceKey, CycleCalendar.nominalDate(
                competence.getYear(), competence.getMonthValue()), generated.size());
    }

    @Transactional
    public ApprovalSummary approveCycle(String competence, DevActor actor) {
        if (!actor.hasRole(Role.ADM)) {
            throw new ForbiddenOperationException("Somente ADM pode aprovar pagamentos");
        }
        List<Payment> payments = paymentRepository.findByCompetenceOrderByCreatedAtAsc(competence);
        if (payments.isEmpty()) {
            throw new CycleNotFoundException(competence);
        }
        for (Payment payment : payments) {
            PaymentStatus previous = payment.getStatus();
            if (previous == PaymentStatus.G) {
                payment.markPending();
                auditEventService.recordPayment(actor.actor(), "PAYMENT_PENDING", payment,
                        previous, payment.getStatus(), json("competence", competence));
                previous = payment.getStatus();
            }
            if (previous == PaymentStatus.P) {
                payment.markSent();
                auditEventService.recordPayment(actor.actor(), "PAYMENT_SENT", payment,
                        previous, payment.getStatus(), json("competence", competence));
            }
        }
        Cnab240GeneratorService.CnabFile cnab = cnabGeneratorService.generate(competence, payments);
        return new ApprovalSummary(competence, cnab.filename(), cnab.recordCount(), cnab.totalAmount(), cnab.content());
    }

    @Transactional(readOnly = true)
    public List<Payment> listByCompetence(YearMonth competence) {
        return paymentRepository.findByCompetence(competence.toString());
    }

    @Transactional(readOnly = true)
    public List<Payment> listByCompetence(String competence) {
        return paymentRepository.findByCompetenceOrderByCreatedAtAsc(competence);
    }

    private Payment generatePayment(String competenceKey, YearMonth competence, SeedBeneficiary beneficiary, DevActor actor) {
        EligibilityDecision documents = eligibilityService.evaluateDocuments(beneficiary.cpf(), beneficiary.documentsOk());
        if (!documents.eligible()) {
            throw new IllegalStateException("Beneficiario seedado inelegivel: " + documents.reason());
        }
        BigDecimal regionalFactor = eligibilityService.isRegionBypass(beneficiary.regionCode())
                ? BigDecimal.ONE
                : beneficiary.regionalFactor();
        var calculation = benefitCalculator.compute(new CalculationContext(
                beneficiary.cpf(), beneficiary.regionCode(), beneficiary.familySize(), beneficiary.ageYears(),
                beneficiary.baseAmount(), regionalFactor, beneficiary.familyFactor(),
                beneficiary.incomeFactor(), beneficiary.ageFactor()));
        JudicialDiscountResult discount = judicialDiscountService.apply(
                calculation.grossAmount(), beneficiary.requestedJudicialDiscount());
        BigDecimal netAmount = calculation.grossAmount().subtract(discount.appliedDiscount());
        Payment payment = paymentRepository.save(new Payment(beneficiary.cpf(), beneficiary.programCode(), competenceKey,
                calculation.grossAmount(), netAmount, CycleCalendar.nominalDate(competence.getYear(), competence.getMonthValue()),
                beneficiary.bankCode(), beneficiary.agency(), beneficiary.account()));
        auditEventService.recordPayment(actor.actor(), "PAYMENT_CREATED", payment, null, payment.getStatus(),
                json("source", "seed-beneficiary"));
        if (eligibilityService.isRegionBypass(beneficiary.regionCode())) {
            auditEventService.recordPayment(actor.actor(), "EXCEPTION_APPLIED", payment, payment.getStatus(), payment.getStatus(),
                    json("reason", "region in exception list"));
        }
        if (documents.bypassReason() != null) {
            auditEventService.recordPayment(actor.actor(), "CPF_PREFIX_BYPASS", payment, payment.getStatus(), payment.getStatus(),
                    json("reason", documents.bypassReason()));
        }
        if (discount.capped()) {
            auditEventService.recordPayment(actor.actor(), "JUDICIAL_DISCOUNT_CAPPED", payment, payment.getStatus(), payment.getStatus(),
                    json("requested", discount.requestedDiscount().toPlainString()));
        }
        return payment;
    }

    private static String json(String key, String value) {
        return "{\"" + key + "\":\"" + value.replace("\"", "'") + "\"}";
    }

    public record CycleSummary(String competence, LocalDate nominalDate, int generatedCount) {}
    public record ApprovalSummary(String competence, String filename, int recordCount, BigDecimal totalAmount, String content) {}
}
