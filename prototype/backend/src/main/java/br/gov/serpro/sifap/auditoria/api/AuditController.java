package br.gov.serpro.sifap.auditoria.api;

import br.gov.serpro.sifap.auditoria.application.AuditEventService;
import br.gov.serpro.sifap.auditoria.domain.AuditEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditEventService service;

    public AuditController(AuditEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditEventDto> recent() {
        return service.recent().stream().map(AuditEventDto::from).toList();
    }

    public record AuditEventDto(UUID id, String actor, String action, UUID paymentId,
                                String prevState, String newState, OffsetDateTime occurredAt, String payload) {
        static AuditEventDto from(AuditEvent event) {
            return new AuditEventDto(event.getId(), event.getActor(), event.getAction(), event.getPaymentId(),
                    event.getPrevState(), event.getNewState(), event.getOccurredAt(), event.getPayload());
        }
    }
}