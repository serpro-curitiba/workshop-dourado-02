package br.gov.serpro.sifap.pagamento.application;

import br.gov.serpro.sifap.pagamento.domain.Payment;
import br.gov.serpro.sifap.pagamento.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Orquestracao do ciclo mensal de pagamentos.
 *
 * Implementa:
 *   REQ-PAY-001 (BR-007): so abre ciclo no 5o dia util.
 *   REQ-PAY-011 (BR-010): regime de dezembro.
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN
 */
@Service
public class PaymentCycleService {

    private final PaymentRepository paymentRepository;

    public PaymentCycleService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public CycleSummary openCycle(YearMonth competence, LocalDate today) {
        String competenceKey = competence.toString(); // YYYY-MM
        if (paymentRepository.existsByCompetence(competenceKey)) {
            throw new CycleAlreadyExistsException(competenceKey);
        }
        LocalDate earliest = CycleCalendar.fifthBusinessDay(competence.getYear(), competence.getMonthValue());
        if (today.isBefore(earliest)) {
            throw new CycleTooEarlyException(competenceKey, earliest, today);
        }
        // Geracao real (REQ-PAY-002) virara aqui — itera beneficiarios elegiveis,
        // chama BenefitCalculator e persiste Payment.G em batch.
        List<Payment> generated = List.of();
        return new CycleSummary(competenceKey, CycleCalendar.nominalDate(
                competence.getYear(), competence.getMonthValue()), generated.size());
    }

    @Transactional(readOnly = true)
    public List<Payment> listByCompetence(YearMonth competence) {
        return paymentRepository.findByCompetence(competence.toString());
    }

    public record CycleSummary(String competence, LocalDate nominalDate, int generatedCount) {}
}
