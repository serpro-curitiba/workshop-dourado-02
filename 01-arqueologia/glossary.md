<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Glossário do SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **glossary**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Preencha esta tabela com todos os termos, abreviações e siglas encontrados no código Natural/Adabas.
> **Meta: no mínimo 30 termos.**

## Por que isso importa

Sistemas legados têm vocabulário próprio que ninguém documenta em lugar nenhum — só está no nome das variáveis. Se o time do Estágio 2 não souber o que `DSCT`, `BENF`, `PE` ou `CTC` significam, vai escrever uma spec sobre o que ele _acha_ que isso significa. Glossário é o que evita esse desencontro.

## Como preencher

- **Termo**: a abreviação ou sigla exatamente como aparece no código
- **Expansão**: o significado completo do termo
- **Programa**: em qual arquivo `.NSN` ou `.ddm` o termo foi encontrado
- **Contexto**: breve explicação de como/onde o termo é usado

## Dica de extração

Prompt útil no Copilot Chat (cole o conteúdo de 2–3 arquivos `.NSN` no chat antes):

> _"Liste todas as abreviações e siglas usadas neste código Natural. Para cada uma, sugira a expansão e marque com 'CONFIRMADO' ou 'HIPÓTESE'."_

## Termos encontrados

> Extraídos via leitura dos 15 programas Natural + 4 DDMs Adabas. Total: 65 termos. Fonte preserva `arquivo:linha` para confirmar uso. Termos marcados ⚠ têm semântica ambígua ou indefinida.

### Entidades de domínio

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 1   | `BENF` | Beneficiário | `CADBENEF.NSN`, `BENEFICIARIO.ddm` | Pessoa física registrada como destinatária de programa social. PK = CPF. |
| 2   | `DEPEND` | Dependente | `CADDEPEND.NSN`, `BENEFICIARIO.ddm` (grupo PE `DA`) | Pessoa vinculada a beneficiário titular; máx 5 (CADDEPEND) ou 10 (DDM) ocorrências — ⚠ divergência. |
| 3   | `PROG` | Programa Social | `CADPROG.NSN`, `PROGRAMA-SOCIAL.ddm` | Política pública parametrizada (ex: PBF, BPC, PETI). ~45 ativos em 2018. |
| 4   | `PGTO` | Pagamento | `BATCHPGT.NSN`, `PAGAMENTO.ddm` | Transação mensal a beneficiário. Arquivo 152, ~180M registros (abr/2018). |
| 5   | `AUDIT` | Auditoria | `RELAUDIT.NSN`, `AUDITORIA.ddm` | Trilha imutável (IN-TCU 63/2010). ~25M registros. INSERT-ONLY desde 1998. |
| 6   | `NIS` | Número de Inscrição Social | `CADBENEF.NSN`, `CONSBENF.NSN` | Identificador alternativo ao CPF (Cadastro Único). |
| 7   | `CPF` | Cadastro de Pessoa Física | (todos) | 11 dígitos. Validado por MOD 11 (algoritmo brasileiro). |
| 8   | `CTPS` | Carteira de Trabalho e Previdência Social | `VALDOCS.NSN` | Campo lido mas nunca validado. |

### Códigos de status e tipos (enumerações)

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 9   | `SIT-BENEF`/`STATUS` (A/S/C/I/D) | Ativo / Suspenso / Cancelado / Inativo / Desligado | `VALBENEF.NSN#L207-L213`, `BENEFICIARIO.ddm` | ⚠ `S` ambíguo: em `CADBENEF.NSN#L155-L158` significa idoso (>75 anos); em outros, suspensão genérica. |
| 10  | `SIT-PGTO` (G/P/C/D/E/X/R) | Gerado / Pago / Cancelado / Devolvido / Estornado / Cancelado-2 / Reprocessado | `BATCHREL.NSN#L112-L120`, `PAGAMENTO.ddm` | Máquina de estados implícita (não há diagrama). |
| 11  | `TIPO-PROG` (A/P/T) | Assistencial / Previdenciário / Trabalho | `CADPROG.NSN#L51`, `VALELEG.NSN#L124-L145` | Determina critérios de elegibilidade (renda, idade). |
| 12  | `TIPO-PGTO` (N/D) | Normal / Dezembro (com 13º) | `BATCHPGT.NSN#L257-L274` | Cálculo divergente em dezembro. |
| 13  | `TIPO-DSCT` (J/P/I/S/A/C) | Judicial / Pensão / Imposto / Sindical / Administrativo / Contribuição | `CALCDSCT.NSN#L10, L102-L126` | ⚠ Tipo `C` declarado mas nunca tratado. |
| 14  | `PARENTESCO` (FI/CO/IR/OU) | Filho / Cônjuge / Irmão / Outro | `CADDEPEND.NSN#L75-L79` | ⚠ DDM cita `FI/CJ/NT/TU` — divergência. |
| 15  | `OPERACAO` (I/A/C) | Inclusão / Alteração / Consulta | `CADBENEF.NSN#L85-L89`, `CADPROG.NSN#L45-L48` | Comando de tela 3270. |
| 16  | `ACAO` auditoria (IN/AL/EX/CO/CN/DV/LG/LO/BT/ER/AU/RE) | Inclusão, Alteração, Exclusão, Conciliação, Consulta, Divergência, Login, Logout, Batch, Erro, Auditoria, Reprocessamento | `AUDITORIA.ddm`, `RELAUDIT.NSN#L95-L112` | ⚠ `CO`=Consulta deixou de ser gravado em 2010 (Decisão CGTI 213/2010). |
| 17  | `PERFIL` (ADM/OPR/CON/AUD/SUP) | Administrador / Operador / Consulta / Auditor / Supervisor | `AUDITORIA.ddm` campo `EC` | Autorização. |

### Cálculo financeiro

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 18  | `VLR-BRUTO` | Valor bruto antes de descontos | `BATCHPGT.NSN`, `CALCBENF.NSN`, `PAGAMENTO.ddm` campo `BA` | N9.2. Resultado de cinco fatores multiplicativos. |
| 19  | `VLR-LIQUIDO` | Valor líquido (bruto − descontos) | `CALCBENF.NSN#L181-L187`, `PAGAMENTO.ddm` campo `BB` | Nunca negativo (`IF < 0 MOVE 0`). |
| 20  | `VLR-BASE` | Valor base individual do programa | `PROGRAMA-SOCIAL.ddm` campo `BA` | Parâmetro do programa, multiplicado por fatores. |
| 21  | `FAT-REG` / `FATOR-REGIONAL` | Fator regional | `BATCHPGT.NSN#L186-L214` | Tabela de 27 índices (estados); 26-27 reserva = 1.0. |
| 22  | `FAT-FAM` / `FATOR-FAMILIAR` | Fator familiar (dependentes) | `BATCHPGT.NSN#L216-L227` | 0 deps=1.0; 1-2=1.0+(dep×0.05); 3-4=1.1+((dep-2)×0.03); 5+=1.16+((dep-4)×0.02). |
| 23  | `FAT-RND` / `FATOR-RENDA` | Fator de renda familiar | `BATCHPGT.NSN#L229-L234` | ⚠ "RND" não documentado. 5 faixas: ≤300=1.0, ≤600=0.85, ≤1000=0.7, ≤1500=0.55, >1500=0.4. |
| 24  | `FAT-IDADE` | Fator idade | `BATCHPGT.NSN#L236-L246` | <18=1.05; 18-59=1.0; 60-64=1.1; 65+=1.15. |
| 25  | `FAT-REAJ` | Fator de reajuste | `PROGRAMA-SOCIAL.ddm` campo `BE` | % anual (3.2). |
| 26  | `FATOR-K` ⚠ | Fator de correção especial — **NÃO DOCUMENTADO** | `PROGRAMA-SOCIAL.ddm` campo `BG` | Inserido Ago/2008 sem especificação. Comentário diz "ATENDE SOLICITACAO SENARC". |
| 27  | `0.347215` ⚠ | Fator de conversão K em CADPROG | `CADPROG.NSN#L65` | Constante mágica sem origem documentada. |
| 28  | `IPCA` | Índice Nacional de Preços ao Consumidor Amplo | `CALCCORR.NSN#L18-L67` | Inflação. ⚠ Tabelas hardcoded só cobrem 2010-2012. |
| 29  | `13o` / `Décimo Terceiro` | Salário adicional de dezembro | `BATCHPGT.NSN#L258-L274` | `BASE × FAT_REG × FAT_IDADE`. |
| 30  | `Abono Natalino` | Adicional 15% em dezembro para tipo `A` | `BATCHPGT.NSN#L268`, `CALCBENF.NSN#L169` | Constante `0.15`. |
| 31  | `Contribuição Social` | Desconto obrigatório por faixa de bruto | `CALCDSCT.NSN#L39-L48` | ≤500=3%, ≤1000=5%, ≤2000=7%, >2000=9%. |
| 32  | `Teto de Desconto` | Limite 30% do bruto | `CALCDSCT.NSN#L77` | ⚠ Desconto judicial (`J`) **não tem teto**. |
| 33  | `Renda Familiar` | Renda mensal declarada | `BENEFICIARIO.ddm` campo `CH` (N9.2) | Usada em fator de renda e elegibilidade. |
| 34  | `Renda per Capita` | Renda familiar / nº membros | `BENEFICIARIO.ddm` campo `CJ`, `PROGRAMA-SOCIAL.ddm` campo `CA` | Critério de elegibilidade. |
| 35  | `Competência` | Período de referência AAAAMM | `BATCHPGT.NSN`, `PAGAMENTO.ddm` campo `AE` | N6. |

### Conciliação bancária

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 36  | `CNAB 240` | Centro Nacional de Automação Bancária — layout 240 colunas | `BATCHCON.NSN` | Padrão FEBRABAN de retorno bancário. |
| 37  | `COD-RET` (00/01/02) | Código retorno banco: Pago / Devolvido / Estornado | `BATCHCON.NSN#L127-L151` | |
| 38  | `Divergência` | Diferença > R$ 0,01 entre valor enviado e retornado | `BATCHCON.NSN#L117-L121` | Gera registro de auditoria com ACAO='DV'. |
| 39  | `Conciliação` | Casamento entre PAGAMENTO e retorno bancário | `BATCHCON.NSN` | Auditoria ACAO='CO'. |
| 40  | `SIAFI` | Sistema Integrado de Administração Financeira (federal) | `PAGAMENTO.ddm` campos `FA-FE` | Integração com tesouro nacional. |
| 41  | `OB-SIAFI` | Ordem Bancária SIAFI | `PAGAMENTO.ddm` campo `FA` | Identificador da ordem. |
| 42  | `NE-SIAFI` | Nota de Empenho SIAFI | `PAGAMENTO.ddm` campo `FB` | |
| 43  | `UG` | Unidade Gestora | `PAGAMENTO.ddm` campo `FC` | Órgão emissor no SIAFI. |
| 44  | `FEBRABAN` | Federação Brasileira de Bancos | `PAGAMENTO.ddm` campo `EA` | Define padrão de códigos bancários. |

### Validação

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 45  | `MOD 11` | Algoritmo de dígito verificador módulo 11 | `VALBENEF.NSN`, `VALDOCS.NSN`, `CADBENEF.NSN#L98-L104` | Usado para CPF. |
| 46  | `CPF Especial` ⚠ | Prefixos 000, 001, 002, 010, 011, 099, 100, 999 — **validação bypass** | `VALDOCS.NSN#L122-L130` | Auto-valida ignorando dígitos. Risco de segurança. |
| 47  | `Região 99` ⚠ | Código de região especial (internacional/diplomático) | `VALELEG.NSN#L79-L83` | **Bypass de TODAS as regras de elegibilidade**. |
| 48  | `DOCUMENTOS-OK` ⚠ | Indicador de documentação completa | `VALELEG.NSN`, `VALDOCS.NSN` | Referenciado nos programas mas **não existe no DDM `BENEFICIARIO`**. |

### Estrutura Adabas / Natural

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 49  | `DDM` | Data Definition Module | (todos) | Esquema lógico Adabas (equivalente DDL). |
| 50  | `FDT` | Field Definition Table | (DDMs) | Lista de campos físicos. |
| 51  | `DE` | Descriptor | (DDMs) | Índice simples Adabas. |
| 52  | `MU` | Multiple-value field | `BENEFICIARIO.ddm` `EA-EC`, `AUDITORIA.ddm` `DB-DF` | Campo multi-valor (até N ocorrências). |
| 53  | `PE` | Periodic Group | `BENEFICIARIO.ddm` `DA`, `PAGAMENTO.ddm` `CA`, `PROGRAMA-SOCIAL.ddm` `DA/FA` | Grupo periódico (até N repetições estruturadas). |
| 54  | `ISN` | Internal Sequence Number | Adabas | Identificador físico do registro. |
| 55  | `Super-descriptor` (S1/S2/S3) | Índice composto Adabas | `BENEFICIARIO.ddm`, `PAGAMENTO.ddm`, `AUDITORIA.ddm` | Concatenação de campos para busca. |
| 56  | `CALLNAT` | Chamada de subprograma Natural | (Natural keyword) | Equivalente a `call function`. |
| 57  | `READ LOGICAL` | Leitura ordenada por descritor | (Natural keyword) | |
| 58  | `FIND` | Busca por descritor | (Natural keyword) | |
| 59  | `STORE` / `UPDATE` | Persistência | (Natural keyword) | |
| 60  | `ESCAPE TOP` | Pular para topo do loop (continue) | `BATCHPGT.NSN#L127-L132` | |
| 61  | `ARQ 150/151/152/153/155` | File Number Adabas | (todos DDMs) | 150=BENEFICIARIO, 151=PAGAMENTO ou PROGRAMA, 152=PAGAMENTO, 153=AUDITORIA, 155=PROGRAMA-SOCIAL. ⚠ numerações divergem entre programas. |

### Domínio temporal e regulatório

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 62  | `IN-TCU 63/2010` | Instrução Normativa TCU sobre auditoria | `AUDITORIA.ddm` comentários | Base legal da trilha imutável. |
| 63  | `Lei 8159/1991` | Lei de Arquivos Públicos | `AUDITORIA.ddm` | Retenção 10 anos. |
| 64  | `SENARC` | Secretaria Nacional de Renda de Cidadania | `PROGRAMA-SOCIAL.ddm` FATOR-K | Origem do FATOR-K (Ago/2008). |
| 65  | `Plano Verão` | Plano econômico 1989-1991 (Cruzado → Cruzeiro) | `CALCCORR.NSN#L70-L73` | Código comentado mantido por valor histórico. |

> 65 termos confirmados via leitura de código. Termos ⚠ requerem clarificação com especialistas do domínio antes do Estágio 2.

## Exemplo de linha bem preenchida

| #   | Termo  | Expansão | Programa                        | Contexto                                                                                                         |
| --- | ------ | -------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 1   | `DSCT` | Desconto | `CALCDSCT.NSN`, `PAGAMENTO.ddm` | Tipo de dedução aplicada sobre valor bruto do pagamento. Tipos: 'J' (judicial), 'I' (imposto), 'T' (trabalhista) |

## Observações

- Anote aqui qualquer padrão de nomenclatura que o time identificou:
- Convenções de prefixo/sufixo encontradas:
- Termos ambíguos que precisam de validação com especialista:

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
<a href="business-rules-catalog.md"><strong>business-rules-catalog.md</strong></a><br/>
<sub>Catálogo de regras.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

