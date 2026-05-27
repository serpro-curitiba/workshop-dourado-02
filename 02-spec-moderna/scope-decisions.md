<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Decisões de Escopo — SIFAP 2.0

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S2](https://img.shields.io/badge/PREENCHA-Durante%20S2-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 2](README.md) → **Scope Decisions**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 2 (Spec Moderna).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento preenchido para sua feature
> 2. Rastreabilidade `source_legacy:` para cada REQ-ID
> 3. Sign-off do Product Owner antes da passagem H2
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Para cada funcionalidade encontrada no Estágio 1, decida: **Migrar**, **Descartar** ou **Evoluir**.
>
> - **Migrar**: trazer para o SIFAP 2.0 como está (mesma lógica, nova tecnologia)
> - **Descartar**: não trazer — funcionalidade obsoleta ou desnecessária
> - **Evoluir**: trazer E melhorar (nova UX, novo fluxo, nova capacidade)

**Time**: Workshop Dourado-02 (single-operator)
**Data**: 27/05/2026
**Edição**: 1
**Par 1 (Product Owner) responsável**: Operador único cobrindo as 10 personas

## Por que isso importa

O escopo é o que protege o time de chegar às 17h00 com 12 features pela metade. Se o Par 1 não cortar, o Estágio 3 não fecha. **Decisão difícil é tomada aqui, não no Estágio 3.**

## Como decidir

Pergunte de cada funcionalidade:

1. **Afeta o ciclo mensal de pagamento?** Sim → Migrar. Não → considere descartar.
2. **Tem uso documentado nos últimos 12 meses?** Não → descartar.
3. **Faz parte de um relatório regulatório obrigatório (TCU, CGU, BB)?** Sim → Migrar como está.
4. **Tem uma versão moderna mais barata de implementar?** Sim → Evoluir.

---

## Decisões por Funcionalidade

| #   | Funcionalidade            | Decisão  | Justificativa | Regra de Negócio (BR-XXX) | Prioridade |
| --- | ------------------------- | -------- | ------------- | ------------------------- | ---------- |
| 1   | Cadastro de Beneficiário (CADBENEF) | **Evoluir** | Entidade central; corrige bug de máscara CPF (BR-028/MYS-009) e separa semântica de status `S` (BR-013/BR-016/MYS-012) | BR-013, BR-016, BR-017, BR-028 | Alta |
| 2   | Cadastro de Dependentes (CADDEPEND) | **Evoluir** | Unificar limite real (5) vs DDM (10); resolver divergência de parentesco FI/CO/IR/OU vs FI/CJ/NT/TU | BR-017 | Média |
| 3   | Cadastro de Programa Social (CADPROG) | **Evoluir** | Expõe e documenta `0.347215` (MYS-001) e `FATOR-K` (MYS-007); externalizar tabelas hardcoded | BR-018, BR-019, BR-024 | Alta |
| 4   | Consulta de Beneficiário (CONSBENF) | **Evoluir** | UI moderna corrige máscara de CPF; restaurar logging de ACAO='CO' por LGPD (BR-026) | BR-028, BR-026 | Alta |
| 5   | Validação de CPF (VALDOCS) | **Evoluir** | Remover bypass de prefixos (BR-012/MYS-003) salvo lista justificada de exceções; manter MOD 11 | BR-012 | Alta |
| 6   | Validação de Elegibilidade (VALELEG) | **Evoluir** | Tornar região 99 (BR-003/MYS-004) lista de exceções controlada e auditada | BR-003, BR-018 | Alta |
| 7   | Validação de Status do Beneficiário (VALBENEF) | **Migrar** | Máquina de estados explícita preservando 5 status; corrigir Fev=29 hardcoded (BR-020/MYS-002) | BR-013, BR-014, BR-020 | Alta |
| 8   | Cálculo de Benefício (CALCBENF) | **Migrar** | Núcleo financeiro — preservar fórmula BR-001 com testes paralelos shadow-run vs legado | BR-001, BR-002, BR-004, BR-005, BR-006, BR-008 | Alta |
| 9   | Correção Monetária (CALCCORR) | **Evoluir** | Substituir tabela IPCA hardcoded (MYS-006) por integração SGS/BCB | BR-015 | Alta |
| 10  | Cálculo de Descontos (CALCDSCT) | **Evoluir** | Manter regra (BR-007) mas adicionar log de "valor descartado por teto" (MYS-010); remover tipo 'C' órfão (BR-029) | BR-007, BR-008, BR-009 | Alta |
| 11  | Geração de Ciclo de Pagamento (BATCHPGT) | **Migrar** | Pipeline central de ~180M pagamentos/mês; preservar regime de dezembro (BR-010/BR-011); precisa shadow-run | BR-001, BR-010, BR-011, BR-014, BR-024 | Alta |
| 12  | Conciliação Bancária CNAB 240 (BATCHCON) | **Evoluir** | Adicionar limite de retry + DLQ ao loop de estorno (BR-022); idempotência | BR-021, BR-022 | Alta |
| 13  | Geração de Relatórios (BATCHREL) | **Evoluir** | Unificar política de arredondamento (MYS-013) com CALCBENF | BR-023 | Média |
| 14  | Relatório Operacional de Pagamento (RELPGT) | **Migrar** | Layout aceito pelo TCU; manter | BR-021 | Média |
| 15  | Relatório de Auditoria (RELAUDIT) | **Evoluir** | Mascarar CPF (BR-027) e separar trilha LGPD | BR-025, BR-027 | Alta |
| 16  | Trilha de Auditoria (AUDITORIA) | **Evoluir** | Voltar a registrar ACAO='CO' (LGPD); adicionar política de purge 10 anos (MYS-015) | BR-025, BR-026 | Alta |
| 17  | Tela 3270 (UI operador) | **Descartar** | UI obsoleta; substituir por Next.js | — | Alta |
| 18  | Easter egg Plano Verão (código comentado em CALCCORR) | **Descartar** | Sem valor operacional; preservar apenas em git history | BR-030 | Baixa |
| 19  | Slots de região 26-27 reservados | **Descartar** | Brasil tem 27 estados — fixar tabela | BR-002 | Baixa |
| 20  | Tipo de desconto 'C' (declarado nunca usado) | **Descartar** | Código morto (BR-029) | BR-029 | Baixa |

> Cobertura: todos os 15 programas + 4 DDMs + UI legada.

---

## Funcionalidades Novas (não existem no legado)

> Liste funcionalidades que o SIFAP 2.0 deveria ter e que não existem no sistema legado. Cada uma vira REQ-ID com `source_legacy: [GREENFIELD] <justificativa>`.

| #   | Funcionalidade Nova | Justificativa | Prioridade | Complexidade |
| --- | ------------------- | ------------- | ---------- | ------------ |
| N1  | Autenticação OAuth2 + JWT | Substituir sessão de terminal 3270 por padrão moderno; integração com SSO federal (gov.br) | Alta | Média |
| N2  | API REST `/api/v1/*` com OpenAPI 3.1 | Habilita integração de terceiros (gov.br, dashboards CGU) | Alta | Média |
| N3  | Frontend Next.js 15 (App Router) | Substitui terminal 3270 com UX moderna, acessível (WCAG AA) | Alta | Alta |
| N4  | Logging estruturado JSON + tracing OpenTelemetry | Observabilidade ausente no legado | Alta | Baixa |
| N5  | Mascaramento sistemático de CPF em logs/APIs (LGPD) | Princípio da minimização — não existe no legado | Alta | Baixa |
| N6  | Política automatizada de retenção/purge da AUDITORIA (10 anos, Lei 8159/1991) | Tabela nunca purgada desde 1998 — risco LGPD + custo | Alta | Média |
| N7  | Shadow-run de cálculo (legado vs novo) por 3 ciclos antes de cutover | Garantir equivalência financeira em ~180M pagamentos/mês | Alta | Alta |
| N8  | Integração SGS/BCB para IPCA (substitui tabela hardcoded) | MYS-006: tabelas só vão até 2012 | Alta | Baixa |
| N9  | Dashboard de divergências de conciliação | Visibilidade que hoje só existe no log noturno | Média | Média |
| N10 | Health endpoints (`/actuator/health`, `/readyz`) + métricas Prometheus | Pré-requisito Azure/k8s | Alta | Baixa |

---

## Resumo de Escopo

| Decisão   | Quantidade | Percentual |
| --------- | ---------- | ---------- |
| Migrar    | 4          | 20%        |
| Descartar | 4          | 20%        |
| Evoluir   | 12         | 60%        |
| **Total** | 20         | 100%       |

> +10 funcionalidades novas (greenfield) catalogadas acima.

## Riscos de Escopo

| Risco | Probabilidade | Impacto | Mitigação |
| ----- | ------------- | ------- | --------- |
| Cálculo de benefício divergir do legado (BR-001 + 5 fatores) | Alta | Alto | N7 shadow-run obrigatório por 3 ciclos; testes property-based com dataset legado |
| Resposta do negócio aos mistérios (0.347215, FATOR-K, região 99, CPF prefix) atrasar spec | Alta | Alto | Escalar SENARC/CGU na primeira semana; ter Plano B documentado por escrito |
| Migração de dados (~180M PAGAMENTO + ~25M AUDITORIA) exceder janela de cutover | Média | Alto | Strangler Fig com dual-write por 3 meses; ETL incremental |
| Quebra de compatibilidade com SIAFI/CNAB 240 na conciliação | Média | Crítico | Manter layout binário 240; testes de contrato com banco parceiro |
| LGPD: descobrir que ACAO='CO' nunca logada gerou multa | Alta | Médio | Iniciar logging de consultas imediatamente; comunicar DPO |
| Operadores resistirem à substituição da tela 3270 | Média | Médio | Treinamento + período de operação dupla (terminal + web) |
| Constante `0.347215` ser legalmente exigida sem documentação | Baixa | Alto | Decisão jurídica antes de remover; manter como parâmetro com nome semântico |

## Aprovação

- [x] Par 1 (Product Owner) aprovou as decisões de escopo
- [x] Par 2 (Enterprise Architect) validou a viabilidade técnica
- [x] Par 3 (Technical Lead) confirmou que cabe nas 3 horas do Estágio 3 (apenas slice MVP — ciclo de pagamento)
- [x] Time concordou com as prioridades

> Sign-off do operador único (todas as 10 personas).

— Paula


---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="GUIDE.md"><strong>GUIDE do Estágio 2</strong></a><br/>
<sub>Passo a passo do estágio.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="ADR-TEMPLATE.md"><strong>ADR-TEMPLATE</strong></a><br/>
<sub>Template de ADR.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>

