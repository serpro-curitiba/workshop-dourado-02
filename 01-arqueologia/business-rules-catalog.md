<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Catálogo de Regras de Negócio — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **business-rules-catalog**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Registre aqui todas as regras de negócio extraídas do código Natural/Adabas.
> Cada regra precisa ter rastreabilidade até o código-fonte.
>
> **REGRA DURA:** linhas com `Programa Fonte` vazio são **inválidas** e não contam para o gate do Estágio 2. Use o formato `01-arqueologia/legado-sifap/natural-programs/ARQUIVO.NSN#L<inicio>-L<fim>` sempre que possível. Mínimo aceito: nome do arquivo .NSN.

## Como pensar em "regra de negócio"

O que conta:

- Um `IF` que decide algo no domínio (ex.: _"se a UF é do Nordeste e o programa é Seca, valor base × 1.2"_)
- Uma constante numérica sem explicação (ex.: `0.075` num cálculo de imposto)
- Uma transição de status com regra (ex.: _"só de A para S, nunca de I para A"_)
- Um tratamento especial para um caso (ex.: _"se o CPF começa com 999, é teste"_)

O que NÃO conta: paginação de relatório, formatação de saída, manipulação de cursor Adabas, abertura de arquivo. Ignore esses detalhes de implementação.

## Níveis de Risco

| Nível       | Descrição                                                     |
| ----------- | ------------------------------------------------------------- |
| **CRÍTICO** | Regra financeira ou de segurança — erro causa prejuízo direto |
| **ALTO**    | Regra de negócio central — afeta fluxo principal              |
| **MÉDIO**   | Regra de validação ou formatação — afeta qualidade dos dados  |
| **BAIXO**   | Regra de apresentação ou conveniência — impacto limitado      |

## Regras Encontradas

> 30 regras de maior risco (de ~116 candidatas extraídas dos 15 programas + 4 DDMs). Catálogo completo destila as regras que **devem** ser preservadas ou explicitamente refatoradas no Estágio 2. Cada `source_legacy:` aponta para um intervalo verificável.

| ID     | Regra de Negócio | Programa Fonte | Campos DDM | Nível de Risco | Notas |
| ------ | ---------------- | -------------- | ---------- | -------------- | ----- |
| BR-001 | Valor bruto do pagamento mensal = `BASE × FAT_REG × FAT_FAM × FAT_RND × FAT_IDADE`. Os 5 fatores são multiplicativos; alteração de qualquer um afeta todos os ~180M registros mensais. | `01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L186-L255` | `PROGRAMA-SOCIAL.VLR-BASE`, `PAGAMENTO.VLR-BRUTO`, `BENEFICIARIO.REGIAO`, `BENEFICIARIO.QTD-DEPEND`, `BENEFICIARIO.RENDA-FAMILIAR`, `BENEFICIARIO.DATA-NASC` | CRÍTICO | Núcleo do cálculo. Toda regra abaixo é parametrização desta fórmula. |
| BR-002 | Fator regional usa tabela hardcoded de 27 índices (estados BR). Posições 26-27 são "reserva" e valem 1.0. Região 99 é tratada à parte. | `BATCHPGT.NSN#L186-L214` | `BENEFICIARIO.REGIAO` | CRÍTICO | Tabela imutável desde 2003; não há UI de manutenção. Migrar para tabela de parâmetros versionada. |
| BR-003 | Região = 99 **bypassa TODAS as regras de elegibilidade** (renda, idade, documentação). | `VALELEG.NSN#L79-L83` | `BENEFICIARIO.REGIAO`, `BENEFICIARIO.STATUS-DOCS` | CRÍTICO | Suspeita-se de uso original para diplomáticos/internacionais; sem documentação. **Risco regulatório alto.** Validar com SENARC antes de migrar. |
| BR-004 | Fator familiar: 0 deps=1.0; 1-2 deps=`1.0 + (deps × 0.05)`; 3-4 deps=`1.1 + ((deps-2) × 0.03)`; ≥5 deps=`1.16 + ((deps-4) × 0.02)`. | `BATCHPGT.NSN#L216-L227` | `BENEFICIARIO.QTD-DEPEND` | CRÍTICO | Fórmula com 3 faixas; descontinuidade entre faixas pode gerar incentivo perverso. |
| BR-005 | Fator de renda familiar: ≤300=1.0; ≤600=0.85; ≤1000=0.7; ≤1500=0.55; >1500=0.4. Faixas em **R$ correntes não atualizados desde 2005**. | `BATCHPGT.NSN#L229-L234` | `BENEFICIARIO.RENDA-FAMILIAR` | CRÍTICO | Defasagem de 13 anos vs salário mínimo. Pode estar atribuindo 0.4 a famílias que deveriam ter 1.0. |
| BR-006 | Fator idade: <18 anos=1.05; 18-59=1.0; 60-64=1.1; 65+=1.15. Cálculo em anos completos no dia 1 da competência. | `BATCHPGT.NSN#L236-L246` | `BENEFICIARIO.DATA-NASC`, `PAGAMENTO.COMPETENCIA` | ALTO | |
| BR-007 | Desconto total não pode exceder 30% do bruto, **exceto descontos judiciais (tipo J)** que não têm teto. | `CALCDSCT.NSN#L77, L102-L126` | `PAGAMENTO.VLR-BRUTO`, `PAGAMENTO.VLR-TOTAL-DSCT`, `PAGAMENTO.TIPO-DSCT` | CRÍTICO | Combinado com BR-008 pode zerar valor líquido. Caso real de líquido = R$ 0,00 em 2014. |
| BR-008 | Valor líquido nunca é negativo: `IF VLR-LIQUIDO < 0 MOVE 0 TO VLR-LIQUIDO`. Quando descontos > bruto, diferença é silenciosamente descartada. | `CALCBENF.NSN#L181-L187` | `PAGAMENTO.VLR-LIQUIDO` | CRÍTICO | Não há log de "desconto perdido". Possível passivo trabalhista. |
| BR-009 | Contribuição social compulsória por faixa de bruto: ≤500=3%, ≤1000=5%, ≤2000=7%, >2000=9%. | `CALCDSCT.NSN#L39-L48` | `PAGAMENTO.VLR-BRUTO`, `PAGAMENTO.VLR-CONTRIB` | ALTO | Tabela hardcoded; não atrelada a lei vigente. |
| BR-010 | Em dezembro (`COMPETENCIA MOD 100 = 12`), tipo de pagamento muda para 'D' e é adicionado 13º = `BASE × FAT_REG × FAT_IDADE` (sem FAT_FAM nem FAT_RND). | `BATCHPGT.NSN#L257-L274`, `CALCBENF.NSN#L165-L175` | `PAGAMENTO.COMPETENCIA`, `PAGAMENTO.TIPO-PGTO`, `PAGAMENTO.VLR-13` | CRÍTICO | Fórmula divergente — fácil quebrar se time não conhecer. |
| BR-011 | Abono natalino: programas tipo `A` (assistencial) recebem +15% sobre 13º em dezembro. | `BATCHPGT.NSN#L268`, `CALCBENF.NSN#L169` | `PROGRAMA-SOCIAL.TIPO-PROG`, `PAGAMENTO.VLR-13` | ALTO | Constante `0.15` hardcoded. |
| BR-012 | Validação CPF usa MOD 11 brasileiro **exceto** quando prefixo ∈ {000, 001, 002, 010, 011, 099, 100, 999} — nestes casos auto-valida ignorando dígitos. | `VALDOCS.NSN#L122-L130`, `CADBENEF.NSN#L98-L104` | `BENEFICIARIO.CPF` | CRÍTICO | Risco de fraude. Originalmente para "CPFs corporativos/teste". |
| BR-013 | Status do beneficiário: A=Ativo, S=Suspenso, C=Cancelado, I=Inativo, D=Desligado. Transições válidas: A↔S, A→C, A→I, S→C, S→A. C e D são terminais. | `VALBENEF.NSN#L207-L213`, `CADBENEF.NSN#L155-L158` | `BENEFICIARIO.SIT-BENEF` | ALTO | ⚠ Em `CADBENEF.NSN#L155`, `S` significa "idoso >75 anos" (semântica conflitante). |
| BR-014 | Pagamentos só podem ser gerados para beneficiários com `STATUS = 'A'` E `DOCUMENTOS-OK = 'S'`. | `VALBENEF.NSN#L142-L155`, `BATCHPGT.NSN#L127-L135` | `BENEFICIARIO.STATUS`, `BENEFICIARIO.DOCUMENTOS-OK` | CRÍTICO | ⚠ Campo `DOCUMENTOS-OK` **não existe no DDM** `BENEFICIARIO`. Como funciona? Ver MYS-005. |
| BR-015 | Reajuste anual do valor base = `BASE × (1 + IPCA_acumulado_12m / 100)`. Calculado em janeiro de cada ano. | `CALCCORR.NSN#L80-L93` | `PROGRAMA-SOCIAL.VLR-BASE`, `PROGRAMA-SOCIAL.FAT-REAJ` | CRÍTICO | Tabelas IPCA hardcoded só vão até 2012 (ver MYS-006). |
| BR-016 | Idoso ≥ 75 anos no cadastro recebe automaticamente status 'S' (idoso preferencial), **não** suspensão. | `CADBENEF.NSN#L155-L158` | `BENEFICIARIO.DATA-NASC`, `BENEFICIARIO.SIT-BENEF` | ALTO | Conflito semântico com BR-013. |
| BR-017 | Cadastro de dependente: máx 5 ocorrências por titular no programa, mas DDM permite 10. Parentesco aceito: FI/CO/IR/OU. | `CADDEPEND.NSN#L42-L79` | `BENEFICIARIO.DA` (PE) | ALTO | ⚠ DDM cita parentesco FI/CJ/NT/TU — divergência. |
| BR-018 | Programa social: tipo 'A' = assistencial (sem critério de renda alto), 'P' = previdenciário (idade ≥60), 'T' = trabalho (CTPS ativa). | `CADPROG.NSN#L51`, `VALELEG.NSN#L124-L145` | `PROGRAMA-SOCIAL.TIPO-PROG`, `BENEFICIARIO.IDADE`, `BENEFICIARIO.CTPS` | ALTO | |
| BR-019 | Cálculo de correção monetária usa fator `0.347215` aplicado a valor base em CADPROG. **Origem desconhecida**. | `CADPROG.NSN#L65` | `PROGRAMA-SOCIAL.VLR-BASE` | CRÍTICO | Número mágico. Ver MYS-001. Possivelmente conversão Cruzeiro/Real (1994)? |
| BR-020 | Fevereiro é tratado como tendo **29 dias hardcoded** independente do ano. | `VALBENEF.NSN#L119` | `BENEFICIARIO.DATA-NASC` | ALTO | Bug latente em anos não-bissextos. Ver MYS-002. |
| BR-021 | Conciliação bancária (CNAB 240): retorno 00=Pago, 01=Devolvido, 02=Estornado. Divergência > R$ 0,01 entre enviado e retornado dispara registro de auditoria ACAO='DV'. | `BATCHCON.NSN#L117-L151` | `PAGAMENTO.SIT-PGTO`, `PAGAMENTO.COD-RET-BANCO`, `AUDITORIA.AC` | CRÍTICO | Tolerância 1 centavo. |
| BR-022 | Estorno bancário (cod 02) reverte pagamento para `SIT-PGTO='E'` e cria pagamento de reprocessamento na competência seguinte. | `BATCHCON.NSN#L153-L172` | `PAGAMENTO.SIT-PGTO`, `PAGAMENTO.COMPETENCIA` | CRÍTICO | Pode entrar em loop se estorno persiste. Sem limite de tentativas. |
| BR-023 | Arredondamento de valores: BATCHREL faz `+0.005` antes do truncamento (banker's rounding-like); CALCBENF apenas trunca. **Divergência produz centavos diferentes nos relatórios vs PAGAMENTO real**. | `BATCHREL.NSN#L98-L102` vs `CALCBENF.NSN#L181-L187` | `PAGAMENTO.VLR-LIQUIDO` | ALTO | Discrepância sistemática em relatórios mensais. |
| BR-024 | Programas sociais têm `FATOR-K` (campo BG) aplicado como multiplicador final. **Sem documentação**. Inserido em Ago/2008. | `PROGRAMA-SOCIAL.ddm` campo `BG`, `BATCHPGT.NSN` (uso indireto via leitura do PROG) | `PROGRAMA-SOCIAL.FATOR-K` | CRÍTICO | Atende solicitação SENARC. Ver MYS-007. |
| BR-025 | Auditoria: toda ação `IN/AL/EX` em BENEFICIARIO/PAGAMENTO gera registro em AUDITORIA com usuário, IP, timestamp, valores anterior/posterior. Imutável (INSERT-ONLY). | `RELAUDIT.NSN#L42-L80`, `CADBENEF.NSN#L210-L240` | `AUDITORIA.*` | CRÍTICO | Base legal: IN-TCU 63/2010. |
| BR-026 | Ação `CO` (Consulta) **deixou de ser gravada** desde 2010 (Decisão CGTI 213/2010 — performance). | `CONSBENF.NSN#L82-L88` (comentário) | `AUDITORIA.AC` | ALTO | LGPD-incompatível em 2018+. Ver MYS-008. |
| BR-027 | Relatório de auditoria `RELAUDIT` imprime CPF **sem máscara**. | `RELAUDIT.NSN#L156-L168` | `AUDITORIA.CPF-AFETADO` | CRÍTICO | Violação LGPD direta. |
| BR-028 | Tela de consulta CONSBENF aplica máscara de CPF `***.XXX.XXX-**` — bug conhecido inverte campo (mascara o que deveria mostrar). Audit congelou correção. | `CONSBENF.NSN#L97-L108` (com comentário "NAO CORRIGIR SEM APROVACAO") | `BENEFICIARIO.CPF` | ALTO | Ver MYS-009. |
| BR-029 | Tipo de desconto declarado mas nunca processado: 'C' (contribuição) aparece em `CALCDSCT.NSN#L10` mas não tem branch no `DECIDE`. | `CALCDSCT.NSN#L10, L102-L126` | `PAGAMENTO.TIPO-DSCT` | MÉDIO | Código morto ou feature incompleta. |
| BR-030 | Plano Verão (1989-1991): código de conversão Cruzado→Cruzeiro mantido comentado em CALCCORR como "valor histórico". | `CALCCORR.NSN#L70-L73` | n/a | BAIXO | Não migrar — easter egg. |

> Total: **30 regras catalogadas** (de ~116 candidatas mapeadas). Cobertura: todos os 15 programas, todos os 4 DDMs. As 30 regras listadas concentram o risco financeiro/regulatório/segurança crítico.

## Exemplo de linha bem preenchida

| ID     | Regra de Negócio                                                                        | Programa Fonte                                   | Campos DDM                                                               | Nível de Risco | Notas                                      |
| ------ | --------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------ | -------------- | ------------------------------------------ |
| BR-013 | Desconto total não pode exceder 30% do valor bruto, exceto descontos judiciais (tipo J) | `01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L142-L148` | `PAGAMENTO.VLR-BRUTO`, `PAGAMENTO.VLR-TOTAL-DSCT`, `PAGAMENTO.TIPO-DSCT` | CRÍTICO        | Regra financeira. Tipo 'J' = exceção legal |

## Regras por Categoria

### Cálculos Financeiros

- **Núcleo**: BR-001 (fórmula principal), BR-002 (regional), BR-004 (familiar), BR-005 (renda), BR-006 (idade)
- **Especiais**: BR-010 (13º), BR-011 (abono natalino), BR-019 (fator 0.347215), BR-024 (FATOR-K)
- **Descontos**: BR-007 (teto 30% c/ exceção judicial), BR-008 (líquido nunca negativo), BR-009 (contribuição social), BR-029 (tipo 'C' órfão)
- **Reajuste/Correção**: BR-015 (reajuste anual IPCA), BR-023 (divergência de arredondamento)

### Validações de Status

- **Status beneficiário**: BR-013 (5 estados + transições), BR-016 (idoso 75+ marca 'S')
- **Status pagamento**: BR-022 (estorno reverte para 'E' + reprocessa)
- **Pré-requisitos**: BR-014 (status='A' E documentos-ok)

### Regras de Autorização

- BR-003 (região 99 bypass total) **← maior risco regulatório**
- BR-012 (CPF prefixo bypass) **← maior risco de fraude**
- BR-025 (auditoria IN/AL/EX obrigatória)
- BR-026 (consulta deixou de ser logada — risco LGPD)
- BR-027 (CPF sem máscara em relatórios — violação LGPD)
- BR-028 (máscara CPF com bug congelado)

### Regras de Negócio Temporais

- BR-010 (regime de dezembro)
- BR-015 (reajuste janeiro/anual)
- BR-020 (Fev=29 hardcoded) **← bug latente**
- BR-021 (conciliação mensal CNAB 240)
- BR-022 (reprocessamento N+1)
- BR-030 (Plano Verão — código histórico congelado)

### Cadastro / Estrutura

- BR-017 (dependentes: 5 vs 10 — divergência)
- BR-018 (tipos de programa A/P/T)

## Resumo Estatístico

- Total de regras encontradas: **30 catalogadas** (de ~116 candidatas)
- Regras críticas: **18**
- Regras com duplicação: **3** (BR-002/BR-003 região; BR-007/BR-008 descontos; BR-013/BR-016 status 'S')
- Regras sem documentação (escondidas): **10+** (BR-003, BR-005, BR-008, BR-012, BR-019, BR-020, BR-022, BR-024, BR-026, BR-028)

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="GUIDE.md"><strong>GUIDE do Estágio 1</strong></a><br/>
<sub>Passo a passo do estágio.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="dependency-map.md"><strong>dependency-map.md</strong></a><br/>
<sub>Mapa de quem chama quem.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

