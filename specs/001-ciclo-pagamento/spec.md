<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# SPECIFICATION — Feature 001: Ciclo de Pagamento Mensal

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![Status Pronta-Implementação](https://img.shields.io/badge/STATUS-Pronta%20p%2F%20Implementa%C3%A7%C3%A3o-7FBA00?style=for-the-badge)

**Feature ID**: 001-ciclo-pagamento
**Bounded Context primário**: `pagamento`
**Bounded Contexts secundários**: `cadastro`, `elegibilidade`, `calculo`, `conciliacao`, `auditoria`
**Branch**: `spec/001-ciclo-pagamento`
**Author**: Requirements Engineer (persona 02) + Product Owner (persona 01)
**Reviewers**: Tech Lead (persona 05), QA (persona 08)
**Data**: 27/05/2026

---

## Objetivo

Substituir o batch noturno `BATCHPGT.NSN` por um pipeline Java/Spring que gera o ciclo mensal de pagamentos, calcula valores conforme catálogo de regras (BR-001…BR-030), envia ao banco via CNAB 240, processa retornos, e mantém trilha de auditoria 100% rastreável.

## Escopo

- **In:** Geração de ciclo (G→P), validação de elegibilidade, cálculo de benefício, exportação CNAB 240, importação de retorno, atualização de status, auditoria.
- **Out:** Cadastro de beneficiário (feature 002), relatórios TCU/CGU (feature 003), gov.br SSO (feature 004).

## Requisitos (formato EARS)

```yaml
REQ-PAY-001:
  pattern: state-driven
  text: "Enquanto a competência estiver aberta, o sistema DEVE permitir a geração do ciclo de pagamentos no 5º dia útil do mês."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L42-L78
  business_rule: BR-007
  acceptance:
    - "POST /api/v1/payment-cycles {competence: '2026-06'} no 5º dia útil retorna 201"
    - "POST no 4º dia útil retorna 422 com código CYCLE_TOO_EARLY"
    - "POST quando a competência já tem ciclo retorna 409"
  priority: P0
  risk: CRÍTICO

REQ-PAY-002:
  pattern: ubiquitous
  text: "O sistema DEVE calcular o valor bruto do benefício como BASE × FAT_REG × FAT_FAM × FAT_RND × FAT_IDADE."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L186-L255
  business_rule: BR-001
  acceptance:
    - "Beneficiário com base R$ 600, FAT_REG=1.05, FAT_FAM=1.20, FAT_RND=1.00, FAT_IDADE=1.10 → bruto R$ 831,60"
    - "Cobertura de teste com 50 cenários do snapshot de produção (shadow-run)"
    - "Divergência vs Adabas ≤ R$ 0,01 em 99,9% dos casos"
  priority: P0
  risk: CRÍTICO

REQ-PAY-003:
  pattern: event-driven
  text: "Quando o cálculo aplicar arredondamento, o sistema DEVE usar HALF_UP em escala 2 (NUMERIC(15,2))."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L240-L248
  business_rule: BR-013
  acceptance:
    - "Valor R$ 100,505 → R$ 100,51 (não R$ 100,50)"
    - "Resolve MYS-013 (arredondamento inconsistente do legado)"
  priority: P0
  risk: ALTO

REQ-PAY-004:
  pattern: unwanted
  text: "Se o beneficiário estiver na lista de exceções de região (tabela region_exception), o sistema NÃO DEVE aplicar fator regional padrão; DEVE usar fator 1.0 e registrar evento de auditoria EXCEPTION_APPLIED."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L195-L210
  business_rule: BR-003
  acceptance:
    - "Beneficiário com região '99' (legado-bypass) usa fator 1.0"
    - "Evento de auditoria contém: actor=SYSTEM, action=EXCEPTION_APPLIED, reason='region in exception list'"
    - "Resolve MYS-004 (bypass região 99 não documentado)"
  priority: P0
  risk: CRÍTICO

REQ-PAY-005:
  pattern: unwanted
  text: "Se o CPF do beneficiário começar com prefixo listado em cpf_prefix_exception, o sistema NÃO DEVE bloquear o pagamento por documentação faltante; DEVE registrar evento de auditoria CPF_PREFIX_BYPASS."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALDOCS.NSN#L88-L120
  business_rule: BR-012
  acceptance:
    - "CPF '000.XXX.XXX-XX' (prefixo legado) passa validação mesmo sem docs"
    - "Evento de auditoria gravado com motivo explícito"
    - "Resolve MYS-003 (bypass CPF não documentado, descoberto na arqueologia)"
  priority: P0
  risk: CRÍTICO

REQ-PAY-006:
  pattern: event-driven
  text: "Quando o ciclo for confirmado pelo aprovador (perfil ADM), o sistema DEVE gerar arquivo CNAB 240 e mudar status de PAGAMENTO de P→E."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L320-L377
  business_rule: BR-014
  acceptance:
    - "POST /api/v1/payment-cycles/{id}/approve por user com role ADM retorna 200"
    - "POST por user com role OPR retorna 403"
    - "Arquivo CNAB gerado segue layout FEBRABAN 240 v10.7"
    - "Status de cada Payment vira E (Enviado)"
  priority: P0
  risk: ALTO

REQ-PAY-007:
  pattern: event-driven
  text: "Quando o arquivo de retorno bancário (CNAB 240) for importado, o sistema DEVE casar cada registro com seu Payment correspondente via {bank_code + agency + account + amount + nominal_date}."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHCON.NSN#L55-L140
  business_rule: BR-021
  acceptance:
    - "Match exato → Payment vira C (Conciliado), evento PaymentReconciled"
    - "Sem match → registra em reconciliation_divergence, evento ReconciliationDivergenceDetected"
    - "Múltiplos matches → flag conflict=true para revisão manual"
  priority: P0
  risk: ALTO

REQ-PAY-008:
  pattern: unwanted
  text: "Se o mesmo arquivo CNAB for importado duas vezes (mesmo hash SHA-256), o sistema NÃO DEVE reprocessar registros; DEVE retornar 200 com flag duplicate=true."
  source_legacy: "[GREENFIELD] Idempotência ausente no legado — BATCHCON podia processar mesmo arquivo 2x se operador rodasse de novo (BR-022 loop)."
  business_rule: BR-022
  acceptance:
    - "Tabela bank_return_file tem coluna sha256 UNIQUE"
    - "Segundo upload do mesmo arquivo não cria payment_status_history duplicado"
    - "Resolve risco operacional descoberto na arqueologia"
  priority: P0
  risk: ALTO

REQ-PAY-009:
  pattern: event-driven
  text: "Quando qualquer mudança de estado em Payment ocorrer, o sistema DEVE gravar audit_event na mesma transação (atomicidade ACID)."
  source_legacy: 01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm
  business_rule: BR-025
  acceptance:
    - "Insert em audit_event ocorre no mesmo @Transactional do update de payment"
    - "Se audit falhar, a mudança de estado também é revertida"
    - "audit_event tem: actor, action, payment_id, prev_state, new_state, occurred_at, payload (JSONB)"
  priority: P0
  risk: CRÍTICO

REQ-PAY-010:
  pattern: ubiquitous
  text: "O sistema DEVE gravar evento ACAO='CO' (consulta) em audit_event para toda leitura de dados pessoais via API."
  source_legacy: "[GREENFIELD] LGPD Art. 37 — log de operações de tratamento. Resolve MYS-008 (ação CO presente no DDM mas não gravada pelo legado desde 2010)."
  business_rule: BR-026
  acceptance:
    - "GET /api/v1/payments/{id} gera evento action=READ com actor=usuário JWT"
    - "Bulk export gera 1 evento por registro (não por request)"
    - "Auditor consulta /api/v1/audit e vê histórico de quem consultou cada beneficiário"
  priority: P1
  risk: ALTO

REQ-PAY-011:
  pattern: state-driven
  text: "Enquanto a competência for dezembro, o sistema DEVE aplicar regime de pagamento antecipado (data nominal = 20/12 em vez de 5º dia útil)."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L92-L115
  business_rule: BR-010
  acceptance:
    - "Ciclo de competência 2026-12 tem data_nominal = 2026-12-20"
    - "Demais competências mantêm 5º dia útil"
    - "Documentado em runbook (operador precisa entender por que dezembro é diferente)"
  priority: P0
  risk: MÉDIO

REQ-PAY-012:
  pattern: complex
  text: "Quando uma decisão judicial de desconto for cadastrada, o sistema DEVE aplicar o desconto SE o valor do desconto ≤ 70% do bruto; SENÃO DEVE aplicar limite de 70% e registrar evento JUDICIAL_DISCOUNT_CAPPED."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L60-L95
  business_rule: BR-019
  acceptance:
    - "Desconto judicial R$ 200 sobre bruto R$ 500 → aplica R$ 200 (40%)"
    - "Desconto judicial R$ 400 sobre bruto R$ 500 → aplica R$ 350 (70%) + evento de cap"
    - "Resolve MYS-010 (legado não tinha cap — risco fiscal de bloquear pagamento integral)"
  priority: P0
  risk: CRÍTICO

REQ-PAY-013:
  pattern: event-driven
  text: "Quando o cálculo precisar de IPCA acumulado, o sistema DEVE consultar SGS/BCB série 433 em tempo real (com cache de 24h)."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCCORR.NSN#L30-L75
  business_rule: BR-020
  acceptance:
    - "Chamada HTTP a https://api.bcb.gov.br/dados/serie/bcdata.sgs.433"
    - "Cache Redis com TTL 24h"
    - "Fallback: se BCB indisponível, usa último valor cacheado e emite ALERT"
    - "Resolve MYS-006 (IPCA congelado em 2012 no legado)"
  priority: P1
  risk: ALTO

REQ-PAY-014:
  pattern: ubiquitous
  text: "O sistema DEVE mascarar CPF em todas as respostas de API exceto para perfis AUD (auditor) e ADM (administrador)."
  source_legacy: "[GREENFIELD] LGPD Art. 6º (princípio da minimização). Resolve MYS-009 (legado tinha bug de máscara que foi congelado por compatibilidade)."
  business_rule: BR-027
  acceptance:
    - "Perfil OPR vê '***.***.123-**'"
    - "Perfil AUD vê CPF integral + evento de auditoria action=READ_PII"
    - "Perfil ADM vê CPF integral + evento de auditoria"
  priority: P0
  risk: ALTO
```

## Resumo de Cobertura

| Métrica | Valor |
| --- | --- |
| Total de REQ-IDs | 14 |
| Com `source_legacy` apontando para legado | 11 |
| Greenfield com justificativa | 3 (REQ-PAY-008, 010, 014) |
| BRs cobertas | BR-001, 003, 007, 010, 012, 013, 014, 019, 020, 021, 022, 025, 026, 027 (14 de 30) |
| Mistérios resolvidos | MYS-003, 004, 006, 008, 009, 010, 013 (7 de 15) |
| Cobertura por prioridade | P0=12, P1=2 |
| Cobertura por risco | CRÍTICO=6, ALTO=7, MÉDIO=1 |

## Dependências e Riscos

- **Pré-requisito**: feature 002 (cadastro de beneficiário) precisa estar pelo menos com modelo de leitura disponível.
- **Risco P0**: integração BCB pode falhar — mitigar com cache + fallback (REQ-PAY-013).
- **Risco P0**: divergência shadow-run vs Adabas — mitigar com 3 ciclos paralelos antes do cutover (ADR-002).
- **Risco P1**: layout CNAB pode mudar — versionamento de parser.

## Validação (Definition of Ready)

- ✅ Todos os REQ-IDs no formato canônico EARS
- ✅ 100% têm `source_legacy:` (11 com path + linha; 3 GREENFIELD com justificativa)
- ✅ Cada REQ-ID tem ≥1 critério de aceitação testável
- ✅ Cada REQ-ID rastreia para business rule do catálogo
- ✅ Prioridade e risco definidos
- ✅ Bounded context primário identificado (`pagamento`)
- ✅ ADR-001/002/003 referenciados
- ✅ Pronto para `/speckit.plan`
