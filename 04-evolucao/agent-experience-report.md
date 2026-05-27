<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Relatório de Experiência com GitHub Copilot Agent

![ESTÁGIO 04 Evolução](https://img.shields.io/badge/ESTÁGIO-04%20Evolução-FFB900?style=for-the-badge) ![Status Preenchido](https://img.shields.io/badge/STATUS-Preenchido-7FBA00?style=for-the-badge)

**Time**: Workshop Dourado-02
**Data**: 27/05/2026
**Participantes**: Par 1 (PO + RE), Par 2 (EA + SA), Par 3 (TL + Dev), Par 4 (DBA + QA), Par 5 (DevOps + TW)

---

## 1. Issues Criadas

### Issue 1 — REQ-PAY-004: Exceção de Região

- **Título**: `[Feature] REQ-PAY-004 — Exceção de Região (bypass fator regional + auditoria)`
- **Arquivo de referência**: [`docs/issue-REQ-PAY-004-region-exception.md`](../docs/issue-REQ-PAY-004-region-exception.md)
- **Descrição resumida**: Tornar explícito e auditado o bypass de fator regional para região `'99'`, descoberto na arqueologia do `BATCHPGT.NSN#L195-L210` (MYS-004).
- **Tempo para escrever a Issue**: ~25 minutos (incluindo leitura do legado e escrita dos critérios de aceitação)

### Issue 2 — REQ-PAY-003: Arredondamento HALF_UP

- **Título**: `[Feature] REQ-PAY-003 — Garantir arredondamento HALF_UP em escala 2 (BR-013)`
- **Descrição resumida**: Adicionar teste de regressão com 50 cenários do snapshot de produção para validar que `BenefitCalculator` usa `HALF_UP` e resolve MYS-013.
- **Tempo para escrever a Issue**: ~15 minutos

---

## 2. PRs Gerados pelo Agent

### PR 1 (da Issue 1 — REQ-PAY-004)

- **Branch**: `impl/req-pay-004-region-exception`
- **Tempo que o Agent levou**: ~8 minutos
- **Arquivos modificados**: 6 (V3 migration, RegionException, RegionExceptionRepository, CalculationContext, BenefitCalculator, BenefitCalculatorTest)
- **Testes criados**: Sim — 2 cenários novos em `BenefitCalculatorTest`
- **Precisou de ajustes manuais?**: Sim — o Agent não adicionou `source_legacy:` no Javadoc de `RegionException.java`; foi necessário adicionar manualmente antes do merge
- **Foi mergeado?**: Sim, após revisão do Par 3 (TL)

### PR 2 (da Issue 2 — REQ-PAY-003)

- **Branch**: `impl/req-pay-003-rounding-regression`
- **Tempo que o Agent levou**: ~5 minutos
- **Arquivos modificados**: 1 (`BenefitCalculatorTest.java`)
- **Testes criados**: Sim — tabela parametrizada com 10 cenários de arredondamento
- **Precisou de ajustes manuais?**: Não
- **Foi mergeado?**: Sim

---

## 3. O que funcionou bem

1. **Entendeu a arquitetura modular**: o Agent criou os arquivos nos pacotes corretos (`elegibilidade.domain`, `calculo.application`) sem precisar de instrução explícita sobre a estrutura de pacotes.
2. **Seguiu o padrão de rastreabilidade**: em 5 dos 6 arquivos, adicionou `source_legacy:` no Javadoc automaticamente — só esqueceu em um.
3. **Gerou a migração Flyway corretamente**: incluiu o `INSERT` de seed para a região `'99'` e usou `IF NOT EXISTS` onde aplicável.
4. **Testes com cenários de borda**: criou cenários para região normal E região de exceção sem precisar pedir explicitamente.

---

## 4. O que surpreendeu o time

1. **Velocidade na Issue 1**: 8 minutos para 6 arquivos coerentes foi surpreendentemente rápido — estimávamos 30 minutos de implementação manual.
2. **Leitura do contexto legado**: o Agent leu o comentário `source_legacy: BATCHPGT.NSN#L195-L210` na Issue e buscou o arquivo legado para entender o contexto antes de gerar código.
3. **Negativo — não perguntou sobre o evento de auditoria**: gerou o log via `System.out` em vez de um evento de domínio estruturado; foi necessário pedir explicitamente para refatorar para um `AuditEvent`.

---

## 5. O que falhou ou decepcionou

1. **`source_legacy:` esquecido em 1 arquivo**: o CI de rastreabilidade teria rejeitado o PR — foi necessário revisão humana antes do merge.
2. **Evento de auditoria como `System.out`**: o Agent não inferiu que "registrar evento de auditoria" significava persistir em `audit_event` — interpretou como log de aplicação.
3. **Não criou o endpoint REST de consulta de exceções**: a spec menciona `/api/v1/exceptions` mas o Agent focou apenas no cálculo, ignorando a interface pública do bounded context.

### Tipos de falha encontrados

- [ ] Código não compilava
- [ ] Testes falhavam
- [x] Não seguiu a arquitetura do projeto (evento de auditoria como log)
- [ ] Imports incorretos ou circulares
- [ ] Lógica de negócio errada
- [x] Faltou tratamento de erros (endpoint de exceções ausente)
- [ ] Credenciais ou dados sensíveis no código
- [ ] Outro

---

## 6. Qualidade dos PRs (nota 1–5)

| Critério                | Nota | Comentário |
|-------------------------|------|------------|
| Corretude do código     | 4    | Lógica correta; evento de auditoria precisou de ajuste |
| Aderência à arquitetura | 3    | Pacotes corretos; evento de auditoria fora do padrão |
| Qualidade dos testes    | 4    | Cenários de borda cobertos; faltou teste do evento |
| Documentação gerada     | 4    | Javadoc presente em 5/6 arquivos |
| Clareza do código       | 5    | Código limpo, records Java 21, sem magic numbers |
| **Média geral**         | **4.0** | |

---

## 7. Você usaria o Agent novamente?

- [x] Sim, para tarefas simples e bem definidas

**Justificativa**: Para features com spec EARS bem escrita, critérios de aceitação claros e bounded context isolado, o Agent entrega 80% do trabalho em 20% do tempo. O valor está em ter a Issue bem escrita — quanto melhor a Issue, melhor o PR. Para lógica de domínio complexa (eventos de auditoria, máquina de estados), ainda é necessária supervisão próxima.

---

## 8. Recomendações para outras equipes

1. **Invista tempo na Issue**: uma Issue com spec EARS, critérios de aceitação e lista de arquivos a criar/modificar reduz drasticamente os ajustes manuais no PR.
2. **Sempre revise `source_legacy:`**: o CI vai rejeitar, mas é melhor pegar na revisão do que no pipeline.
3. **Peça explicitamente o padrão de evento**: se o projeto tem um padrão de evento de domínio, inclua um exemplo na Issue — o Agent não infere padrões arquiteturais implícitos.
4. **Use o Agent para testes de regressão**: é onde ele brilha — dado um conjunto de entradas/saídas esperadas, gera tabelas parametrizadas rapidamente.
5. **Não use o Agent para o primeiro bounded context**: use para o segundo em diante, quando o padrão já está estabelecido no código.

---

## 9. Comparação: Agent vs. Copilot Chat vs. Implementação Manual

| Aspecto     | Modo Agent | Copilot Chat | Manual |
|-------------|------------|--------------|--------|
| Velocidade  | ⭐⭐⭐⭐⭐ (8 min) | ⭐⭐⭐ (20 min) | ⭐⭐ (45 min) |
| Qualidade   | ⭐⭐⭐⭐ (80% pronto) | ⭐⭐⭐⭐ (precisa guiar) | ⭐⭐⭐⭐⭐ (controle total) |
| Controle    | ⭐⭐ (caixa preta) | ⭐⭐⭐⭐ (iterativo) | ⭐⭐⭐⭐⭐ |
| Aprendizado | ⭐⭐ (difícil aprender vendo o Agent) | ⭐⭐⭐⭐ (aprende no diálogo) | ⭐⭐⭐⭐⭐ |
| Quando usar | Feature isolada com spec clara | Exploração, debug, refactor | Lógica de domínio crítica |

---

## 10. Comentários livres

O maior aprendizado do workshop foi que **a qualidade do output do Agent é proporcional à qualidade da spec**. Equipes que pularam a arqueologia e escreveram specs sem `source_legacy:` tiveram PRs rejeitados pelo CI e precisaram voltar ao Estágio 1. Equipes que fizeram a arqueologia direito tiveram Issues claras, e o Agent entregou PRs que passaram no CI na primeira tentativa.

A rastreabilidade obrigatória (`source_legacy:` no CI) foi o mecanismo mais valioso do workshop — não deixou nenhuma equipe "inventar" requisitos sem base no legado.

---

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>
