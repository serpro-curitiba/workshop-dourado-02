package br.gov.serpro.sifap.conciliacao.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankReturnFileRepository extends JpaRepository<BankReturnFile, UUID> {

    Optional<BankReturnFile> findBySha256(String sha256);
}
