package br.gov.serpro.sifap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SIFAP 2.0 — Modular Monolith.
 *
 * Bounded contexts (ver 02-spec-moderna/bounded-contexts.md):
 *   cadastro, elegibilidade, calculo, pagamento, conciliacao,
 *   auditoria, relatorios, iam.
 *
 * ADR-001: Modular Monolith (1 deployable).
 * ADR-002: PostgreSQL 16 + Flyway + Strangler Fig.
 * ADR-003: source_legacy: obrigatorio em toda EARS.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
