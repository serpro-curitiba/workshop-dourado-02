<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Mistérios Encontrados — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **mysteries-found**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Registre aqui toda lógica, comportamento ou código que o time não conseguiu explicar.
> "Mistérios" são trechos de código sem documentação, com lógica não-óbvia ou que parecem workarounds.
>
> **Cota mínima para passar pelo portão do Estágio 2:** 5 mistérios documentados.

## O que conta como "mistério"?

- Código que faz algo inesperado sem comentário explicando por quê
- Valores hardcoded sem explicação (números mágicos)
- Lógica condicional que parece um workaround ou gambiarra
- Campos no DDM que não são usados por nenhum programa
- Programas que existem mas não são chamados por ninguém
- Comportamento diferente entre o que a documentação diz e o que o código faz
- Easter eggs deixados pelos desenvolvedores originais

## Níveis de Confiança

| Nível     | Significado                                         |
| --------- | --------------------------------------------------- |
| **ALTA**  | Temos certeza de que há algo estranho aqui          |
| **MÉDIA** | Parece suspeito, mas pode ter explicação            |
| **BAIXA** | Pode ser intencional, mas não conseguimos confirmar |

## Mistérios Catalogados

| ID      | Descrição | Onde Encontrado | Impacto Potencial | Confiança |
| ------- | --------- | --------------- | ----------------- | --------- |
| MYS-001 | Constante mágica `0.347215` aplicada como multiplicador em cálculo de correção. Sem comentário ou documentação. | `CADPROG.NSN#L65` | Erro de cálculo afetando todos os programas sociais; possível resíduo da conversão Cruzeiro Real (1994) | ALTA |
| MYS-002 | Fevereiro é assumido com 29 dias hardcoded independente do ano | `VALBENEF.NSN#L119` | Bug latente: cálculos de idade/aniversário podem falhar silenciosamente em anos não-bissextos | ALTA |
| MYS-003 | CPFs com prefixo ∈ {000, 001, 002, 010, 011, 099, 100, 999} bypassam validação MOD 11 | `VALDOCS.NSN#L122-L130` | Risco crítico de segurança/fraude: permite cadastro com CPF inválido | ALTA |
| MYS-004 | Região = 99 bypassa todas as regras de elegibilidade (renda, idade, documentação) | `VALELEG.NSN#L79-L83` | Risco regulatório alto: beneficiários "região 99" recebem sem checagem | ALTA |
| MYS-005 | Campo `DOCUMENTOS-OK` referenciado em `VALBENEF` e `BATCHPGT` como pré-requisito de pagamento, mas **não existe no DDM** `BENEFICIARIO` | `VALBENEF.NSN#L142-L155`, `BATCHPGT.NSN#L127-L135`; ausente em `BENEFICIARIO.ddm` | Comportamento atual desconhecido — pode estar usando default null que avalia como falso, ou tabela auxiliar não documentada | ALTA |
| MYS-006 | Tabelas IPCA hardcoded no programa de correção monetária cobrem apenas 2010-2012; o que acontece para outros anos? | `CALCCORR.NSN#L18-L67` | Reajustes anuais em janeiro podem usar valor zero/lixo desde 2013 | ALTA |
| MYS-007 | Campo `FATOR-K` (PROGRAMA-SOCIAL.BG) inserido em Ago/2008 sem especificação; comentário diz apenas "ATENDE SOLICITACAO SENARC" | `PROGRAMA-SOCIAL.ddm` campo `BG` | Multiplicador final do valor base — opacidade total sobre lógica de negócio aplicada a ~180M pagamentos/mês | ALTA |
| MYS-008 | Ação `CO` (Consulta) deixou de ser gravada em AUDITORIA desde 2010 (comentário cita "PORT CGTI 213/2010 — perf") | `CONSBENF.NSN#L82-L88` | LGPD-incompatível para 2018+: não há rastro de quem acessou dados pessoais | ALTA |
| MYS-009 | Bug conhecido de máscara de CPF em CONSBENF — comentário "NAO CORRIGIR SEM APROVACAO DA AUDIT" congela o defeito desde 2014 | `CONSBENF.NSN#L97-L108` | Exposição parcial de CPF na tela; correção bloqueada por processo organizacional | MÉDIA |
| MYS-010 | Desconto judicial (tipo 'J') não tem teto, podendo zerar líquido (BR-007/BR-008 combinados). Caso real em 2014. | `CALCDSCT.NSN#L102-L107` | Passivo trabalhista latente: parte do desconto é silenciosamente descartada | ALTA |
| MYS-011 | Tipo de desconto 'C' (contribuição) declarado em `DEFINE DATA` mas nenhum branch do `DECIDE` trata; código morto ou feature incompleta | `CALCDSCT.NSN#L10, L102-L126` | Inconsistência declarativa; provável remoção segura | MÉDIA |
| MYS-012 | Status `S` tem dois significados conflitantes: "suspenso" em VALBENEF; "idoso >75 anos" em CADBENEF | `VALBENEF.NSN#L207-L213` vs `CADBENEF.NSN#L155-L158` | Risco semântico na migração; relatórios podem somar dois grupos não relacionados | ALTA |
| MYS-013 | Arredondamento diverge entre `BATCHREL` (`+0.005` antes do truncamento) e `CALCBENF` (truncamento direto) | `BATCHREL.NSN#L98-L102`, `CALCBENF.NSN#L181-L187` | Relatórios mostram valores diferentes do banco; reconciliação manual mensal | ALTA |
| MYS-014 | DDM `BENEFICIARIO` permite 10 ocorrências em PE `DA` (dependentes); programa `CADDEPEND` limita a 5 | `CADDEPEND.NSN#L42`, `BENEFICIARIO.ddm` | 5 slots órfãos no Adabas; podem ter dados de versões anteriores nunca migrados | MÉDIA |
| MYS-015 | Arquivo AUDITORIA (~25M registros, 153) nunca foi purgado desde 1998. Lei 8159/1991 exige só 10 anos. | `AUDITORIA.ddm` (comentários de criação), histórico operacional | Custo de storage + risco LGPD (dados além do prazo legal) | ALTA |

## Detalhamento dos Mistérios

### MYS-001: Constante mágica `0.347215`

- **Arquivo**: `01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L65`
- **Trecho de código**:

```natural
* APLICA FATOR DE CONVERSAO
COMPUTE VALOR-AJUSTADO = VALOR-BASE * 0.347215
```

- **O que esperávamos**: Uma constante nomeada, com comentário citando origem (lei, decreto, fórmula)
- **O que o código faz**: Multiplica o valor base por um número mágico sem explicação
- **Hipótese do time**: Fator residual da conversão **Cruzeiro Real → Real** (jul/1994, paridade 1 URV = CR$ 2.750). Mas `0.347215 ≈ 1/2.880` não bate exatamente. Pode ser fator de uma reforma monetária anterior (Cruzado novo, Plano Verão) congelado e nunca removido.
- **Risco se ignorarmos**: Todos os valores base de programas sociais ficam multiplicados por uma constante desconhecida. **Migrar com `0.347215` literal preserva o bug; migrar sem ele muda valores de milhões de pagamentos.** Decisão precisa ser de negócio + jurídico.

---

### MYS-003: CPF prefix bypass (risco crítico)

- **Arquivo**: `VALDOCS.NSN#L122-L130`
- **Trecho de código**:

```natural
IF CPF-PREFIXO = '000' OR = '001' OR = '002' OR
                = '010' OR = '011' OR = '099' OR
                = '100' OR = '999'
  MOVE 'S' TO CPF-VALIDO
  ESCAPE ROUTINE
END-IF
```

- **O que esperávamos**: Validação MOD 11 universal
- **O que o código faz**: Auto-aprova CPFs com prefixos específicos
- **Hipótese do time**: Originalmente para "CPFs de teste corporativos" da Receita ou ambientes piloto. Nunca removido. Permite fraude se um operador conhece a brecha.
- **Risco se ignorarmos**: Migrar como está = perpetuar vulnerabilidade. Migrar removendo = quebrar pagamentos legítimos que dependam de algum desses CPFs (verificar quantos beneficiários têm prefixos da lista).

---

### MYS-004: Região 99 bypass total

- **Arquivo**: `VALELEG.NSN#L79-L83`
- **Trecho de código**:

```natural
IF REGIAO = 99
  MOVE 'S' TO ELEGIVEL
  ESCAPE ROUTINE
END-IF
```

- **O que esperávamos**: Região tratada apenas como índice geográfico
- **O que o código faz**: Região 99 ignora **toda** validação de elegibilidade
- **Hipótese do time**: Diplomáticos, brasileiros no exterior, ou caso institucional especial. Sem documentação no SENARC.
- **Risco se ignorarmos**: Auditoria CGU pode questionar pagamentos "região 99". Esclarecer escopo antes de Estágio 2.

---

### MYS-005: Campo `DOCUMENTOS-OK` fantasma

- **Arquivo**: `VALBENEF.NSN#L142-L155`, `BATCHPGT.NSN#L127-L135`
- **Trecho de código**:

```natural
IF BENEFICIARIO.STATUS = 'A' AND BENEFICIARIO.DOCUMENTOS-OK = 'S'
  PERFORM PROCESSAR-PAGAMENTO
END-IF
```

- **O que esperávamos**: Campo `DOCUMENTOS-OK` definido no DDM `BENEFICIARIO`
- **O que o código faz**: Referencia campo que não está no FDT do arquivo 150
- **Hipótese do time**: (a) Campo removido em refactor incompleto; (b) trigger/view Adabas não documentada; (c) campo derivado calculado em outro programa
- **Risco se ignorarmos**: Comportamento atual da gate de pagamento é desconhecido. Precisa investigação no Adabas em produção.

---

### MYS-006: Tabelas IPCA congeladas em 2012

- **Arquivo**: `CALCCORR.NSN#L18-L67`
- **Trecho de código**:

```natural
DEFINE DATA LOCAL
01 #IPCA(N6.2/12)  INIT <5.91, 6.50, 5.84>
* ... (apenas índices 2010, 2011, 2012)
END-DEFINE
```

- **O que esperávamos**: Tabela mantida anualmente ou consulta a sistema externo
- **O que o código faz**: Hardcoded apenas 3 anos
- **Hipótese do time**: Programa foi atualizado em 2012-2013 e nunca mais. Possivelmente substituído por job manual.
- **Risco se ignorarmos**: Se ainda executado, reajustes ≥2013 usam zero ou lixo.

---

### MYS-010: Desconto judicial sem teto

- **Arquivo**: `CALCDSCT.NSN#L102-L107`
- **Trecho de código**:

```natural
IF TIPO-DSCT = 'J'
  COMPUTE VLR-DSCT-TOTAL = VLR-DSCT-TOTAL + VLR-DSCT-ATUAL
* Sem checagem de teto
END-IF
```

- **O que esperávamos**: Cap de 30% como nos demais tipos
- **O que o código faz**: Soma sem limite — quando descontos judiciais superam o bruto, BR-008 zera silenciosamente
- **Hipótese do time**: Originalmente correto (ordem judicial sobrepõe limites administrativos), mas combinação com BR-008 cria perda silenciosa
- **Risco se ignorarmos**: Beneficiário não sabe que parte da pensão judicial foi descartada; passivo trabalhista

---

> Demais mistérios (MYS-002, 007, 008, 009, 011-015) seguem mesmo padrão de evidência. Detalhar conforme necessário durante revisão do Estágio 2.

## Easter Eggs

1. ☑ **Plano Verão (1989-1991)** — Código de conversão Cruzado→Cruzeiro mantido **comentado** em `CALCCORR.NSN#L70-L73` como "valor histórico". Não executa, mas preservado por nostalgia.
2. ☑ **Posições 26-27 reservadas na tabela de regiões** — Brasil tem 27 estados; código aceita até 30 com valores=1.0. Comentário em `BATCHPGT.NSN#L210` diz "RESERVADO PARA NOVOS ESTADOS" (criado em 2003, antes da PEC dos territórios federais).
3. ☑ **Comentário assinado em CONSBENF.NSN#L99**: `* NAO CORRIGIR SEM APROVACAO DA AUDIT - JCM 14/03/2014` (iniciais do desenvolvedor + data) — bug congelado por processo.

## Resumo

- Total de mistérios encontrados: **15** (cota mínima: 5)
- Confiança alta: **11**
- Confiança média: **4**
- Confiança baixa: **0**
- Easter eggs encontrados: **3 / 3** ✓

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="mysteries-checklist.md"><strong>mysteries-checklist.md</strong></a><br/>
<sub>Lista do que procurar.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="discovery-report.md"><strong>discovery-report.md</strong></a><br/>
<sub>Síntese final.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

