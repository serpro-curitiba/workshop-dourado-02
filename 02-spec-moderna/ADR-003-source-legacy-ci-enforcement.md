<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# ADR-003: Rastreabilidade obrigatória legado → spec via `source_legacy:` no CI

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![Status Aceita](https://img.shields.io/badge/STATUS-Aceita-7FBA00?style=for-the-badge)

**Data**: 27/05/2026
**Status**: Aceita
**Decisores**: Requirements Engineer (persona 02), Tech Lead (persona 05), DevOps Engineer (persona 09), QA Engineer (persona 08)

## Contexto

Na edição anterior do workshop, vários times escreveram specs baseadas apenas no brief de modernização, **pulando a leitura do legado**. Resultado: protótipos perderam regras de negócio reais do SIFAP (29 anos de evolução). Em particular, as regras BR-001 a BR-030 do `business-rules-catalog.md` (e os 15 mistérios em `mysteries-found.md`) seriam silenciosamente quebrados se não houvesse rastreabilidade obrigatória.

O `copilot-instructions.md` deste repo já estabelece que toda EARS deve ter `source_legacy:`, mas **uma regra sem enforcement automatizado vira sugestão ignorada sob pressão de prazo**.

Restrições:

- O CI roda em GitHub Actions.
- Testes JUnit/Vitest precisam rastrear para REQ-IDs (comentário inline `// REQ-PAY-001` ou tag `@requirement("REQ-PAY-001")`).
- Specs ficam em `specs/NNN-feature/spec.md` no formato YAML (8 exemplos em `08-exemplos/SPECIFICATION-exemplo.md`).
- REQ-IDs greenfield existem e são legítimos (ex: OAuth2, LGPD) — não podem ser bloqueados, mas precisam de justificativa explícita.

## Opções Consideradas

### Opção 1: Confiar na disciplina do time (sem enforcement)

- **Descrição**: Manter `copilot-instructions.md` como guia, depender de code review.
- **Vantagens**: Zero esforço de implementação; flexibilidade.
- **Desvantagens**: Falhou na edição anterior; sob pressão, ninguém revisa rastreabilidade; débito técnico cresce silencioso.

### Opção 2: Linter local opcional + pre-commit hook

- **Descrição**: Script Python que valida YAML; instalado via `pre-commit`.
- **Vantagens**: Feedback rápido localmente.
- **Desvantagens**: Pre-commit é facilmente burlável (`--no-verify`); CI continua aceitando.

### Opção 3: GitHub Action obrigatório `legacy-traceability` + validador semântico

- **Descrição**: 
  1. Workflow GitHub Actions `.github/workflows/legacy-traceability.yml` rodando em todo PR para `develop`/`main`.
  2. Script (`scripts/validate-traceability.py`) faz 4 checks por REQ-ID:
     - (a) Campo `source_legacy:` existe e não está vazio.
     - (b) Se aponta para `01-arqueologia/legado-sifap/...`, o arquivo deve existir e (se incluir `#L<start>-L<end>`) as linhas devem existir.
     - (c) Se é `[GREENFIELD]`, deve ter texto após o marcador (justificativa).
     - (d) `acceptance:` tem pelo menos 1 critério.
  3. Validador rejeita PR se qualquer REQ-ID falhar; comenta no PR listando os REQ-IDs problemáticos.
  4. Branch protection rule torna o check obrigatório para merge.
- **Vantagens**: Enforcement automático, impossível de burlar via `--no-verify`, feedback claro no PR, alinhado ao workflow `develop → stage → main`.
- **Desvantagens**: Esforço inicial de escrita do validador (~50 LOC Python); falsos positivos em refactor de paths de legado (mitigar com cache + commit message `[legacy-move]` que pula validação de paths).

## Decisão

**Decidimos implementar a Opção 3** — GitHub Action obrigatório `legacy-traceability` rodando em todo PR para `develop`/`stage`/`main`, com validação semântica em 4 checks.

Formato canônico de `source_legacy:` (uma das 3 formas):

```yaml
# Forma A — referência a programa Natural com faixa de linhas
source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L186-L255

# Forma B — referência a DDM Adabas
source_legacy: 01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm

# Forma C — greenfield com justificativa
source_legacy: "[GREENFIELD] LGPD Art. 6º (princípio da minimização) — não há equivalente no legado."
```

REQ-IDs sem `source_legacy:` válido → PR **rejeitado**.

Testes JUnit/Vitest devem ter comentário `// requirement: REQ-PAY-001` na linha imediatamente anterior ao `@Test`/`it(...)` — validador opcional `test-traceability.py` (warning, não block) gera relatório de cobertura REQ-ID → teste.

## Justificativa

1. **Prevenção do problema real** observado na edição anterior (specs sem ancoragem).
2. **Enforcement no CI** é o único nível que sobrevive à pressão de prazo.
3. **Validação semântica** (não só sintática) — verificar que o arquivo existe e as linhas batem evita ponteiros quebrados.
4. **Greenfield permitido** com justificativa preserva flexibilidade para features novas (OAuth2, LGPD, observabilidade).
5. **Comentário no PR** orienta o autor a corrigir em vez de só falhar.
6. **Alinhado com Spec-Kit**: `/speckit.specify` gera o YAML; `/speckit.analyze` pode usar este validador como input.

## Consequências

### Positivas

- Spec sem rastreabilidade torna-se literalmente inviável (impossível de fazer merge).
- Auditor externo (TCU/CGU) consegue rastrear qualquer regra moderna até a linha do legado.
- O catálogo `business-rules-catalog.md` (30 BRs) vira fonte de verdade citada por dezenas de REQ-IDs.
- Reforça leitura do legado (sem ler, não consegue escrever spec válida).

### Negativas

- **Atrito inicial** para devs que querem "só implementar" — mitigar com mensagem de erro educacional (link para `LEGACY-EXPLORATION-CHECKLIST.md`).
- **Refactor de paths do legado** invalida ponteiros — mitigar com convenção `[legacy-move]` no commit message + atualização em massa scriptada.
- **Validação de range de linhas** pode quebrar se linhas mudarem no `.NSN` — mitigar congelando o snapshot do legado em `01-arqueologia/legado-sifap/` (read-only, nunca refactorado).

### Riscos

- **Risco**: validador ter bug que rejeita PRs válidos. **Plano**: testes unitários do validador no próprio CI; flag de emergência `[skip-traceability]` para hotfix com aprovação manual de Tech Lead.
- **Risco**: greenfield virar atalho para evitar leitura do legado. **Plano**: code review obrigatório quando >30% dos REQ-IDs de um PR são `[GREENFIELD]` — exige sign-off do PO ou Requirements Engineer.

## Referências

- [`../.github/copilot-instructions.md`](../.github/copilot-instructions.md) — regras de Spec-Driven Development
- [`../01-arqueologia/LEGACY-EXPLORATION-CHECKLIST.md`](../01-arqueologia/LEGACY-EXPLORATION-CHECKLIST.md) — HARD GATE Estágio 1→2
- [`../01-arqueologia/business-rules-catalog.md`](../01-arqueologia/business-rules-catalog.md) — 30 BRs catalogadas
- [`../08-exemplos/SPECIFICATION-exemplo.md`](../08-exemplos/SPECIFICATION-exemplo.md) — formato YAML de exemplo
- [GitHub Branch Protection Rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [Spec-Kit `/speckit.analyze`](https://github.com/github/spec-kit)
