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

| ID      | Resposta ao checklist | Onde Encontrado | Impacto Potencial | Confiança |
| ------- | --------------------- | --------------- | ----------------- | --------- |
| MYS-001 | `CADBENEF` altera silenciosamente o status para `S` quando a idade calculada é maior que 75 anos | `CADBENEF.NSN#L151-L157` | Mistura classificação etária com situação operacional; outro fluxo interpreta `S` como suspenso | ALTA |
| MYS-002 | `CADDEPEND` limita inclusão a 5 dependentes, mas o DDM `BENEFICIARIO` permite 10 ocorrências no grupo periódico | `CADDEPEND.NSN#L61-L65`, `BENEFICIARIO.ddm#L59-L66` | Cinco posições podem existir no Adabas sem serem mantidas pela tela atual | MÉDIA |
| MYS-003 | Constante mágica `0.347215` entra no cálculo de `#FATOR-K` sem origem documentada | `CADPROG.NSN#L87-L89` | Alterar ou preservar o fator muda valor-base de programas sociais sem justificativa auditável | ALTA |
| MYS-004 | Em dezembro, o cálculo muda para `TIPO-PGTO = 'D'`, adicionando 13º salário e abono natalino para programa assistencial | `CALCBENF.NSN#L242-L260`, `BATCHPGT.NSN#L291-L307` | Migração que trate dezembro como mês normal subpaga beneficiários | ALTA |
| MYS-005 | Valores monetários são truncados após multiplicar por 100, sem arredondamento financeiro | `CALCBENF.NSN#L232-L235`, `BATCHPGT.NSN#L283-L286`, `CALCDSCT.NSN#L181-L184` | Perda sistemática de centavos e divergência com relatórios que arredondam | ALTA |
| MYS-006 | Desconto judicial (`J`) ignora o teto de 30% aplicado aos demais descontos | `CALCDSCT.NSN#L130-L142`, `CALCDSCT.NSN#L166-L170` | Pode consumir todo o benefício; decisão jurídica precisa ser explícita | ALTA |
| MYS-007 | CPFs com prefixos `000`, `001`, `002`, `010`, `011`, `099`, `100` e `999` são aceitos como documento especial | `VALDOCS.NSN#L45-L53`, `VALDOCS.NSN#L166-L177` | Bypass de validação MOD 11 pode permitir cadastro inválido ou de teste em produção | ALTA |
| MYS-008 | Região `99` encerra a rotina de elegibilidade como elegível antes das demais verificações | `VALELEG.NSN#L105-L110` | Beneficiários da região especial pulam renda, idade, status e documentação | ALTA |
| MYS-009 | `BATCHPGT` processa por CPF, ordem menos intuitiva que programa/região, porque downstream passou a depender disso | `BATCHPGT.NSN#L194-L199` | Reordenar o batch pode quebrar conciliação, arquivos externos ou auditorias históricas | MÉDIA |
| MYS-010 | Eventos de auditoria `EX` são lidos, contados como filtrados e nunca exibidos nos relatórios | `RELAUDIT.NSN#L105-L111` | Exclusões ficam ocultas da trilha operacional, ainda que existam no arquivo de auditoria | ALTA |

## Detalhamento dos Mistérios

### MYS-001: Status `S` para maiores de 75 anos

- **Arquivo**: `01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L151-L157`
- **O que o código faz**: Ao incluir ou alterar beneficiário, calcula a idade e move `S` para `#STATUS` quando `#IDADE > 75`.
- **Por que é mistério**: No DDM, `S` significa suspenso; aqui o mesmo código representa um critério demográfico.
- **Risco se ignorarmos**: A migração pode suspender indevidamente idosos ou perder uma classificação etária escondida.

### MYS-002: Limite 5 no programa, limite 10 no DDM

- **Arquivo**: `CADDEPEND.NSN#L61-L65`, `BENEFICIARIO.ddm#L59-L66`
- **O que o código faz**: Bloqueia inclusão quando `#NUM-DEP > 5`, embora o grupo periódico de dependentes tenha 10 ocorrências.
- **Risco se ignorarmos**: Dados antigos nas posições 6-10 podem ser descartados ou ficar invisíveis na modernização.

### MYS-003: Constante mágica `0.347215`

- **Arquivo**: `CADPROG.NSN#L87-L89`
- **O que o código faz**: Calcula `#FATOR-K = 1.00 + (#FATOR-REAJ * 0.347215)` sem explicar a origem do fator.
- **Risco se ignorarmos**: Preservar ou remover o número muda valores de programas sociais sem base normativa clara.

### MYS-004: Dezembro tem fórmula própria

- **Arquivo**: `CALCBENF.NSN#L242-L260`, `BATCHPGT.NSN#L291-L307`
- **O que o código faz**: Em dezembro, muda o tipo de pagamento para `D`, calcula 13º e adiciona abono de 15% para programas tipo `A`.
- **Risco se ignorarmos**: Um cálculo mensal genérico não reproduz a folha de dezembro.

### MYS-005: Truncamento monetário sistemático

- **Arquivo**: `CALCBENF.NSN#L232-L235`, `BATCHPGT.NSN#L283-L286`, `CALCDSCT.NSN#L181-L184`
- **O que o código faz**: Multiplica por 100, move para variável inteira e divide por 100, truncando centavos.
- **Risco se ignorarmos**: Arredondar no sistema novo pode gerar divergência financeira contra o legado.

### MYS-006: Judicial sem teto

- **Arquivo**: `CALCDSCT.NSN#L130-L142`, `CALCDSCT.NSN#L166-L170`
- **O que o código faz**: Para `TIPO-DSCT = 'J'`, soma o desconto e pula a aplicação do teto de 30%.
- **Risco se ignorarmos**: O benefício líquido pode ser zerado por decisão judicial sem log explícito da exceção.

### MYS-007: Prefixos especiais de CPF

- **Arquivo**: `VALDOCS.NSN#L45-L53`, `VALDOCS.NSN#L166-L177`
- **O que o código faz**: Marca documento especial como válido para prefixos `000`, `001`, `002`, `010`, `011`, `099`, `100` e `999`.
- **Risco se ignorarmos**: Manter o bypass perpetua uma brecha; remover sem análise pode bloquear cadastros legados.

### MYS-008: Região 99 bypassa elegibilidade

- **Arquivo**: `VALELEG.NSN#L105-L110`
- **O que o código faz**: Retorna elegível imediatamente quando `#COD-REG = 99`.
- **Risco se ignorarmos**: A regra especial precisa de decisão de negócio antes de virar requisito moderno.

### MYS-009: Ordem batch por CPF

- **Arquivo**: `BATCHPGT.NSN#L194-L199`
- **O que o código faz**: Lê beneficiários por CPF e registra que sistemas downstream dependem dessa ordenação.
- **Risco se ignorarmos**: Melhorar a ordem por performance ou domínio pode quebrar consumidores externos.

### MYS-010: Exclusões ocultas no relatório de auditoria

- **Arquivo**: `RELAUDIT.NSN#L105-L111`
- **O que o código faz**: Quando `AUDITORIA-V.ACAO = 'EX'`, incrementa filtrados e não exibe o evento.
- **Risco se ignorarmos**: Exclusões podem permanecer invisíveis para auditoria operacional e compliance.

## Easter Eggs

1. ☑ **EGG-001 — Plano Verão (1989-1991)**: bloco comentado em `CALCCORR.NSN#L99-L113`, preservado como histórico da transição Cruzado→Cruzeiro.
2. ☑ **EGG-002 — Documentos especiais**: validação em `VALDOCS.NSN#L166-L177` aceita prefixos de CPF sem MOD 11 real.
3. ☑ **EGG-003 — Banco Real**: integração comentada em `BATCHCON.NSN#L207-L223`, mantida mesmo após aquisição pelo Santander.

## Inconsistências e Achados Bônus

| ID      | Descrição | Onde Encontrado | Observação |
| ------- | --------- | --------------- | ---------- |
| INC-001 | Método de arredondamento do relatório (`+0.005`) diverge do truncamento de cálculo | `BATCHREL.NSN#L98-L102`, `CALCBENF.NSN#L232-L235` | Não conta como mistério adicional; reforça MYS-005 |
| INC-002 | Campo `DOCUMENTOS-OK` é usado em views Natural, mas não aparece no DDM `BENEFICIARIO` | `VALELEG.NSN#L24-L25`, `BENEFICIARIO.ddm` | Gap técnico para investigar antes da modelagem moderna |
| INC-003 | `FATOR-K` existe no DDM com aviso de SENARC, mas não está na documentação funcional | `PROGRAMA-SOCIAL.ddm#L8-L13`, `PROGRAMA-SOCIAL.ddm#L39-L43` | Gap documental, relacionado à fórmula de cálculo |
| INC-004 | DDM permite 10 dependentes e programa mantém 5 | `CADDEPEND.NSN#L61-L65`, `BENEFICIARIO.ddm#L59-L66` | Também é o mistério oficial MYS-002 |

## Resumo

- Total de mistérios encontrados: **10 / 10** (cota mínima: 5)
- Confiança alta: **8**
- Confiança média: **2**
- Confiança baixa: **0**
- Easter eggs encontrados: **3 / 3**
- Inconsistências/bônus catalogados: **4**

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

