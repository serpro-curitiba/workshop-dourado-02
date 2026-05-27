package br.gov.serpro.sifap.auditoria.application;

import br.gov.serpro.sifap.auditoria.domain.AuditEvent;
import br.gov.serpro.sifap.auditoria.domain.AuditEventRepository;
import br.gov.serpro.sifap.pagamento.domain.Payment;
import br.gov.serpro.sifap.pagamento.domain.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditEventService {

    private final AuditEventRepository repository;

    public AuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditEvent record(String actor, String action, UUID paymentId,
                             PaymentStatus previousState, PaymentStatus newState, String payload) {
        return repository.save(new AuditEvent(actor, action, paymentId,
                previousState == null ? null : previousState.name(),
                newState == null ? null : newState.name(), payload));
    }

    public AuditEvent recordPayment(String actor, String action, Payment payment,
                                    PaymentStatus previousState, PaymentStatus newState, String payload) {
        return record(actor, action, payment.getId(), previousState, newState, payload);
    }

    public List<AuditEvent> recent() {
        return repository.findTop100ByOrderByOccurredAtDesc();
    }
}