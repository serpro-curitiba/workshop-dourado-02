<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# ADR-002: Migrar de Adabas para PostgreSQL 16 com estratégia Strangler Fig

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![Status Aceita](https://img.shields.io/badge/STATUS-Aceita-7FBA00?style=for-the-badge)

**Data**: 27/05/2026
**Status**: Aceita
**Decisores**: Enterprise Architect (persona 03), DBA (persona 07), Software Architect (persona 04)

## Contexto

Os 4 DDMs Adabas do legado contêm:

- `BENEFICIARIO` (arq 150) — entidade central, com PE `DA` (até 10 dependentes) e MUs (`EA-EC` telefones, etc.).
- `PROGRAMA-SOCIAL` (arq 155) — parametrização, com `FATOR-K` (MYS-007) e `0.347215` indireto.
- `PAGAMENTO` (arq 152) — ~180M registros, PE `CA` (lançamentos), integração SIAFI.
- `AUDITORIA` (arq 153) — ~25M registros INSERT-ONLY desde 1998, MUs `DB-DF`.

Restrições:

- Mainframe Adabas tem custo de licença alto e equipe DBA aposentando.
- Stack-alvo do workshop é PostgreSQL 16 (definida em `copilot-instructions.md`).
- Cutover precisa de **shadow-run** comparando saídas dos dois bancos por 3 ciclos mensais.
- Grupos periódicos (PE) e multi-value (MU) do Adabas não têm equivalente direto em relacional.
- Lei 8159/1991 exige 10 anos de retenção de auditoria.
- LGPD exige mascaramento + log de consultas (MYS-008).

## Opções Consideradas

### Opção 1: Big-bang cutover (Adabas → PostgreSQL em um final de semana)

- **Descrição**: Migrar todos os ~205M registros em janela de 48h, switch DNS, desligar Adabas.
- **Vantagens**: Operação dupla curta; sem complexidade de sync.
- **Desvantagens**: Risco catastrófico em sistema crítico (~180M pagamentos/mês não podem parar); zero plano de rollback após cutover; impossível shadow-run; bug em produção = caos.

### Opção 2: Manter Adabas + nova camada API sobre Adabas

- **Descrição**: API moderna lê/escreve no Adabas via JDBC driver (existe).
- **Vantagens**: Sem migração de dados.
- **Desvantagens**: Custos de licença permanecem; dívida técnica congelada; impossível resolver mistérios de schema (DOCUMENTOS-OK, FATOR-K) sem refactor relacional; não atende objetivo do workshop.

### Opção 3: Strangler Fig com dual-write incremental por bounded context

- **Descrição**: 
  1. Definir schema PostgreSQL por bounded context (8 schemas conforme ADR-001).
  2. ETL inicial: snapshot de cada DDM → tabela PG correspondente (PE/MU normalizados em tabelas-filhas).
  3. Dual-write: durante 3-6 meses, escritas vão para ambos os bancos via outbox pattern.
  4. Shadow-run de leituras: queries comparadas, divergências logadas.
  5. Cutover gradual por bounded context (auditoria primeiro — menor risco; pagamento último — maior volume).
  6. Adabas vira read-only; depois desligado.
- **Vantagens**: Rollback possível em qualquer momento; shadow-run nativo; permite resolver mistérios de schema durante migração; alinha com Modular Monolith (ADR-001).
- **Desvantagens**: Operação dupla por 6+ meses; complexidade de sync; latência adicional na escrita; precisa monitorar drift.

## Decisão

**Decidimos migrar para PostgreSQL 16 usando Strangler Fig com dual-write incremental (Opção 3).**

Mapeamento estrutural:

| Adabas | PostgreSQL |
| --- | --- |
| Arquivo (DDM) | Schema + tabela principal |
| Campo plano | Coluna (tipos abaixo) |
| MU (multi-value) | Tabela-filha 1:N com FK + ordering column |
| PE (periodic group) | Tabela-filha 1:N com FK + ordering column |
| Super-descriptor | Índice composto + (opcional) coluna gerada |
| Descriptor (DE) | Índice B-tree simples |
| `A` (alfanumérico) | `VARCHAR(n)` ou `TEXT` |
| `N` (numérico) | `NUMERIC(precision, scale)` (financeiro) ou `INTEGER`/`BIGINT` |
| `D` (data) | `DATE` |
| `T` (timestamp) | `TIMESTAMPTZ` (UTC sempre) |

Estratégia de particionamento:

- `payment`: particionado por `competence` (mensal) — facilita purge e queries por mês.
- `audit_event`: particionado por mês de `occurred_at` — habilita política de retenção (DROP PARTITION após 10 anos).

## Justificativa

1. **Risco**: sistema crítico não suporta big-bang; Strangler permite rollback granular.
2. **Shadow-run obrigatório** (N7) só é viável com ambos os bancos vivos.
3. **Resolução de mistérios de schema** (DOCUMENTOS-OK fantasma, FATOR-K opaco) precisa fase de investigação — sem corrida.
4. **PostgreSQL 16** tem particionamento maduro, JSONB para audit event payload, e suporte ACID forte (alinha com ADR-001).
5. **Modular Monolith por contexto** (ADR-001) habilita migrar 1 contexto por vez sem coordenação distribuída.

## Consequências

### Positivas

- Rollback granular: se contexto X falhar, desligar feature flag e voltar ao Adabas para X.
- Shadow-run nativo permite validar cada bounded context isoladamente.
- Particionamento por mês habilita purge de auditoria (resolve MYS-015).
- Schema explícito força resolução de mistérios (DOCUMENTOS-OK, FATOR-K).
- Custos de licença Adabas começam a cair quando primeiros contextos migram.

### Negativas

- **Operação dupla 6+ meses** → custo operacional e mental — mitigar com runbooks de divergência e alertas de drift.
- **Latência adicional na escrita** (outbox + dual-write) — mitigar com escrita assíncrona em Adabas; PostgreSQL é fonte da verdade após cutover.
- **Risco de drift entre bancos** — mitigar com job diário de reconciliação + dashboard.
- **Complexidade de ETL inicial** para PE/MU — mitigar com biblioteca Flyway + scripts de migração testados em ambiente de homologação com snapshot real.

### Riscos

- **Risco**: queries de relatório no PostgreSQL não baterem com Adabas. **Plano**: shadow-run obrigatório por 3 ciclos; auditor compara CSV linha-a-linha; tolerância R$ 0,01 (mesma do CNAB).
- **Risco**: tipos numéricos do Adabas (precisão) divergirem. **Plano**: usar `NUMERIC(15,2)` para valores financeiros sempre; banir `FLOAT`/`DOUBLE` para dinheiro.
- **Risco**: trilha de auditoria gerar inconsistência entre os dois sistemas durante dual-write. **Plano**: usar transactional outbox pattern; auditoria escreve apenas no PostgreSQL desde dia 1 (sistema legado já tinha gap MYS-008).

## Referências

- [ADR-001](ADR-001-modular-monolith.md) — Modular Monolith
- [`bounded-contexts.md`](bounded-contexts.md) — 8 schemas-alvo
- [`../01-arqueologia/legado-sifap/adabas-ddms/`](../01-arqueologia/legado-sifap/adabas-ddms/) — DDMs originais
- BR-025 (auditoria), BR-021 (conciliação)
- MYS-005 (DOCUMENTOS-OK), MYS-007 (FATOR-K), MYS-013 (arredondamento), MYS-015 (purge)
- N6 (política de retenção), N7 (shadow-run), N8 (SGS/BCB substitui IPCA hardcoded)
- ["Strangler Fig Pattern" (Martin Fowler, 2004)](https://martinfowler.com/bliki/StranglerFigApplication.html)
- ["Outbox Pattern" (Chris Richardson)](https://microservices.io/patterns/data/transactional-outbox.html)
