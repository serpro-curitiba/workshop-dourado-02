# [Feature] REQ-PAY-004 — Exceção de Região (bypass fator regional + auditoria)

## Contexto

Durante a arqueologia do legado (`BATCHPGT.NSN#L195-L210`), descobrimos que beneficiários
com código de região `'99'` recebem fator regional `1.0` em vez do fator padrão da tabela
`regional_factor`. Esse bypass existia no Natural sem documentação (MYS-004).

A modernização deve tornar esse comportamento **explícito e auditado**.

## Spec de referência

`specs/001-ciclo-pagamento/spec.md` → **REQ-PAY-004** (BR-003)

```yaml
REQ-PAY-004:
  pattern: unwanted
  text: "Se o beneficiário estiver na lista de exceções de região (tabela region_exception),
         o sistema NÃO DEVE aplicar fator regional padrão; DEVE usar fator 1.0 e registrar
         evento de auditoria EXCEPTION_APPLIED."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L195-L210
  business_rule: BR-003
```

## O que precisa ser implementado

### 1. Migração Flyway — `V3__region_exception.sql`

```sql
-- tabela explícita de exceções de região (resolve MYS-004)
CREATE TABLE region_exception (
    region_code   VARCHAR(10) PRIMARY KEY,
    reason        TEXT        NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
-- seed: bypass legado documentado
INSERT INTO region_exception (region_code, reason)
VALUES ('99', 'Bypass legado BATCHPGT.NSN#L195-L210 — região especial sem fator regional');
```

### 2. Entidade `RegionException` (bounded context `elegibilidade`)

Pacote: `br.gov.serpro.sifap.elegibilidade.domain`

```java
@Entity @Table(name = "region_exception")
public record RegionException(
    @Id String regionCode,
    String reason,
    OffsetDateTime createdAt
) {}
```

### 3. Atualizar `BenefitCalculator`

Em `br.gov.serpro.sifap.calculo.application.BenefitCalculator`:

- Injetar `RegionExceptionRepository`
- Antes de aplicar `ctx.regionalFactor()`, verificar se `ctx.regionCode()` está em `region_exception`
- Se sim: usar `BigDecimal.ONE` como fator regional e publicar evento `EXCEPTION_APPLIED`
- Adicionar campo `regionCode` em `CalculationContext`

### 4. Testes obrigatórios

- `BenefitCalculatorTest`: cenário com região `'99'` → fator 1.0
- `BenefitCalculatorTest`: cenário com região normal → fator da tabela
- Verificar que evento `EXCEPTION_APPLIED` é gerado com campos: `actor=SYSTEM`, `action=EXCEPTION_APPLIED`, `reason`

## Critérios de aceitação (da spec)

- [ ] Beneficiário com região `'99'` usa fator 1.0
- [ ] Evento de auditoria contém: `actor=SYSTEM`, `action=EXCEPTION_APPLIED`, `reason='region in exception list'`
- [ ] Beneficiário com região normal continua usando fator da tabela `regional_factor`
- [ ] Testes passam em `mvn test`
- [ ] Migração Flyway V3 aplica sem erro
- [ ] `source_legacy:` presente em todos os novos arquivos Java

## Arquivos a criar/modificar

```
prototype/backend/src/main/resources/db/migration/V3__region_exception.sql   [CRIAR]
prototype/backend/src/main/java/.../elegibilidade/domain/RegionException.java [CRIAR]
prototype/backend/src/main/java/.../elegibilidade/domain/RegionExceptionRepository.java [CRIAR]
prototype/backend/src/main/java/.../calculo/domain/CalculationContext.java    [MODIFICAR — adicionar regionCode]
prototype/backend/src/main/java/.../calculo/application/BenefitCalculator.java [MODIFICAR]
prototype/backend/src/test/java/.../calculo/application/BenefitCalculatorTest.java [MODIFICAR]
```

## Labels sugeridas

`feature`, `bounded-context:elegibilidade`, `bounded-context:calculo`, `REQ-PAY-004`, `P0`
