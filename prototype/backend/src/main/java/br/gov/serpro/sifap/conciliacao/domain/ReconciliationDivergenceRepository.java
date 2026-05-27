package br.gov.serpro.sifap.conciliacao.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReconciliationDivergenceRepository extends JpaRepository<ReconciliationDivergence, UUID> {
}