package br.gov.serpro.sifap.pagamento.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByCompetence(String competence);
    boolean existsByCompetence(String competence);
}
