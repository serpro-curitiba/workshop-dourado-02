package br.gov.serpro.sifap.pagamento.api;

import br.gov.serpro.sifap.auditoria.application.AuditEventService;
import br.gov.serpro.sifap.conciliacao.application.ReconciliationService;
import br.gov.serpro.sifap.pagamento.application.CycleAlreadyExistsException;
import br.gov.serpro.sifap.pagamento.application.CycleNotFoundException;
import br.gov.serpro.sifap.pagamento.application.CycleTooEarlyException;
import br.gov.serpro.sifap.pagamento.application.CpfMaskingService;
import br.gov.serpro.sifap.pagamento.application.ForbiddenOperationException;
import br.gov.serpro.sifap.pagamento.application.PaymentCycleService;
import br.gov.serpro.sifap.pagamento.domain.Payment;
import br.gov.serpro.sifap.shared.security.DevActor;
import br.gov.serpro.sifap.shared.security.DevActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-cycles")
public class PaymentCycleController {

    private final PaymentCycleService service;
    private final DevActorResolver actorResolver;
    private final CpfMaskingService cpfMaskingService;
    private final AuditEventService auditEventService;
    private final ReconciliationService reconciliationService;

    public PaymentCycleController(PaymentCycleService service, DevActorResolver actorResolver,
                                  CpfMaskingService cpfMaskingService,
                                  AuditEventService auditEventService,
                                  ReconciliationService reconciliationService) {
        this.service = service;
        this.actorResolver = actorResolver;
        this.cpfMaskingService = cpfMaskingService;
        this.auditEventService = auditEventService;
        this.reconciliationService = reconciliationService;
    }

    /** REQ-PAY-001: abrir ciclo da competencia. */
    @PostMapping
    public ResponseEntity<?> open(@RequestBody OpenCycleRequest request, HttpServletRequest httpRequest) {
        try {
            var summary = service.openCycle(YearMonth.parse(request.competence()), LocalDate.now(),
                    actorResolver.resolve(httpRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(summary);
        } catch (CycleAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "CYCLE_ALREADY_EXISTS", "message", e.getMessage()));
        } catch (CycleTooEarlyException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "CYCLE_TOO_EARLY",
                            "message", e.getMessage(),
                            "earliest", e.getEarliest().toString()));
        }
    }

    @GetMapping("/{competence}")
    public ResponseEntity<CycleDetailResponse> detail(@PathVariable String competence, HttpServletRequest request) {
        DevActor actor = actorResolver.resolve(request);
        List<PaymentDto> payments = service.listByCompetence(competence).stream()
            .map(payment -> toDtoWithReadAudit(payment, actor))
                .toList();
        BigDecimal total = payments.stream().map(PaymentDto::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return ResponseEntity.ok(new CycleDetailResponse(competence, payments.size(), total, payments));
    }

    @PostMapping("/{competence}/approve")
    public ResponseEntity<?> approve(@PathVariable String competence, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(service.approveCycle(competence, actorResolver.resolve(request)));
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "FORBIDDEN", "message", e.getMessage()));
        } catch (CycleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "CYCLE_NOT_FOUND", "message", e.getMessage()));
        }
    }

    @PostMapping("/{competence}/reconcile")
    public ResponseEntity<?> reconcile(@PathVariable String competence, @RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) throws IOException {
        return ResponseEntity.ok(reconciliationService.reconcile(competence, file.getBytes(), file.getOriginalFilename(),
                actorResolver.resolve(request)));
    }

    private PaymentDto toDtoWithReadAudit(Payment payment, DevActor actor) {
        auditEventService.recordPayment(actor.actor(), "READ", payment, payment.getStatus(), payment.getStatus(),
                "{\"resource\":\"payment\"}");
        if (actor.role() == br.gov.serpro.sifap.shared.security.Role.ADM
                || actor.role() == br.gov.serpro.sifap.shared.security.Role.AUD) {
            auditEventService.recordPayment(actor.actor(), "READ_PII", payment, payment.getStatus(), payment.getStatus(),
                    "{\"field\":\"beneficiaryCpf\"}");
        }
        return PaymentDto.from(payment, cpfMaskingService.maskForRole(payment.getBeneficiaryCpf(), actor.role()));
    }

    public record OpenCycleRequest(String competence) {}
    public record CycleDetailResponse(String competence, int paymentCount, BigDecimal totalAmount, List<PaymentDto> payments) {}
    public record PaymentDto(UUID id, String beneficiaryCpf, String socialProgramCode, String competence,
                             BigDecimal grossAmount, BigDecimal netAmount, LocalDate nominalDate,
                             String status, String bankCode, String agency, String account) {
        static PaymentDto from(Payment payment, String visibleCpf) {
            return new PaymentDto(payment.getId(), visibleCpf, payment.getSocialProgramCode(), payment.getCompetence(),
                    payment.getGrossAmount(), payment.getNetAmount(), payment.getNominalDate(), payment.getStatus().name(),
                    payment.getBankCode(), payment.getAgency(), payment.getAccount());
        }
    }
}
