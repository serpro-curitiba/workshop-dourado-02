package br.gov.serpro.sifap.pagamento.api;

import br.gov.serpro.sifap.pagamento.application.CycleAlreadyExistsException;
import br.gov.serpro.sifap.pagamento.application.CycleTooEarlyException;
import br.gov.serpro.sifap.pagamento.application.PaymentCycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment-cycles")
public class PaymentCycleController {

    private final PaymentCycleService service;

    public PaymentCycleController(PaymentCycleService service) {
        this.service = service;
    }

    /** REQ-PAY-001: abrir ciclo da competencia. */
    @PostMapping
    public ResponseEntity<?> open(@RequestBody OpenCycleRequest request) {
        try {
            var summary = service.openCycle(YearMonth.parse(request.competence()), LocalDate.now());
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

    public record OpenCycleRequest(String competence) {}
}
