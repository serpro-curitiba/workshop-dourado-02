package br.gov.serpro.sifap.conciliacao.application;

import br.gov.serpro.sifap.auditoria.application.AuditEventService;
import br.gov.serpro.sifap.conciliacao.domain.ReconciliationDivergence;
import br.gov.serpro.sifap.conciliacao.domain.ReconciliationDivergenceRepository;
import br.gov.serpro.sifap.pagamento.domain.Payment;
import br.gov.serpro.sifap.pagamento.domain.PaymentRepository;
import br.gov.serpro.sifap.pagamento.domain.PaymentStatus;
import br.gov.serpro.sifap.shared.security.DevActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReconciliationService {

    private final FileIdempotencyService fileIdempotencyService;
    private final Cnab240ReturnParser parser;
    private final PaymentRepository paymentRepository;
    private final ReconciliationDivergenceRepository divergenceRepository;
    private final AuditEventService auditEventService;

    public ReconciliationService(FileIdempotencyService fileIdempotencyService,
                                 Cnab240ReturnParser parser,
                                 PaymentRepository paymentRepository,
                                 ReconciliationDivergenceRepository divergenceRepository,
                                 AuditEventService auditEventService) {
        this.fileIdempotencyService = fileIdempotencyService;
        this.parser = parser;
        this.paymentRepository = paymentRepository;
        this.divergenceRepository = divergenceRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public ReconciliationSummary reconcile(String competence, byte[] content, String filename, DevActor actor) {
        FileIdempotencyService.ImportResult importResult = fileIdempotencyService.register(content, filename);
        if (importResult.duplicate()) {
            return new ReconciliationSummary(competence, true, 0, 0, 0);
        }
        String text = new String(content);
        List<Cnab240ReturnParser.ReturnRecord> records = parser.parse(text);
        int matched = 0;
        int divergences = 0;
        int conflicts = 0;
        for (Cnab240ReturnParser.ReturnRecord record : records) {
            List<Payment> candidates = paymentRepository.findByBankCodeAndAgencyAndAccountAndNetAmountAndNominalDate(
                    record.bankCode(), record.agency(), record.account(), record.amount(), record.nominalDate());
            if (candidates.size() == 1 && candidates.get(0).getCompetence().equals(competence)) {
                Payment payment = candidates.get(0);
                PaymentStatus previous = payment.getStatus();
                payment.markReconciled();
                auditEventService.recordPayment(actor.actor(), "PAYMENT_RECONCILED", payment,
                        previous, payment.getStatus(), "{\"bankReturnFile\":\"" + filename + "\"}");
                matched++;
            } else if (candidates.isEmpty()) {
                divergenceRepository.save(new ReconciliationDivergence(competence, "NO_MATCH", false,
                        "Sem pagamento para registro " + record.paymentId()));
                auditEventService.record(actor.actor(), "RECONCILIATION_DIVERGENCE_DETECTED", null,
                        null, null, "{\"type\":\"NO_MATCH\"}");
                divergences++;
            } else {
                divergenceRepository.save(new ReconciliationDivergence(competence, "MULTIPLE_MATCHES", true,
                        "Multiplos pagamentos para registro " + record.paymentId()));
                auditEventService.record(actor.actor(), "RECONCILIATION_DIVERGENCE_DETECTED", null,
                        null, null, "{\"type\":\"MULTIPLE_MATCHES\"}");
                divergences++;
                conflicts++;
            }
        }
        return new ReconciliationSummary(competence, false, matched, divergences, conflicts);
    }

    public record ReconciliationSummary(String competence, boolean duplicate, int matched, int divergences, int conflicts) {
    }
}