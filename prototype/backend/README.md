# SIFAP 2.0 — Protótipo (Estágio 3)

Modular Monolith Java 21 + Spring Boot 3.3.

## Estrutura

```
src/main/java/br/gov/serpro/sifap/
├── Application.java          ← Spring Boot entry
├── calculo/                  ← bounded context: Cálculo (REQ-PAY-002/003)
│   ├── domain/
│   └── application/
└── pagamento/                ← bounded context: Ciclo de Pagamento (REQ-PAY-001/011)
    ├── domain/
    ├── application/
    └── api/
```

Demais contextos (`cadastro`, `elegibilidade`, `conciliacao`, `auditoria`, `relatorios`, `iam`) serão adicionados em iterações seguintes — ver [`02-spec-moderna/bounded-contexts.md`](../../02-spec-moderna/bounded-contexts.md).

## Rastreabilidade

Cada classe e teste cita `REQ-PAY-NNN` + `source_legacy:` conforme [ADR-003](../../02-spec-moderna/ADR-003-source-legacy-ci-enforcement.md) e [spec](../../specs/001-ciclo-pagamento/spec.md).

| Arquivo | REQ-ID | BR | source_legacy |
| --- | --- | --- | --- |
| `BenefitCalculator.java` | REQ-PAY-002, 003 | BR-001, BR-013 | `BATCHPGT.NSN#L186-L255` |
| `CycleCalendar.java` | REQ-PAY-001, 011 | BR-007, BR-010 | `BATCHPGT.NSN#L42-L115` |
| `PaymentCycleService.java` | REQ-PAY-001 | BR-007 | `BATCHPGT.NSN` |
| `Payment.java` | REQ-PAY-006 | BR-014 | `BATCHPGT.NSN#L320-L380` |
| `V1__pagamento_module.sql` | — | — | `adabas-ddms/PAGAMENTO.ddm` |

## Rodar testes (sem Docker)

```powershell
cd prototype/backend
mvn test
```

Usa H2 em memória no perfil de teste (ver `src/test/resources/application.yml`).

## Rodar a aplicação (precisa Docker)

```powershell
docker compose up -d   # sobe Postgres na raiz do repo
cd prototype/backend
mvn spring-boot:run
```

Endpoints:
- `POST /api/v1/payment-cycles` — `{"competence": "2026-06"}`
- `GET /actuator/health`
