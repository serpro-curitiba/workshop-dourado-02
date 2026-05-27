package br.gov.serpro.sifap.pagamento.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByCompetence(String competence);
    List<Payment> findByCompetenceOrderByCreatedAtAsc(String competence);
    List<Payment> findByBankCodeAndAgencyAndAccountAndNetAmountAndNominalDate(
            String bankCode, String agency, String account, BigDecimal netAmount, LocalDate nominalDate);
    boolean existsByCompetence(String competence);
}
