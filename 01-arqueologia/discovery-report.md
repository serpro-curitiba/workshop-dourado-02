<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Relatório de Descoberta — Estágio 1: Arqueologia Digital

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **discovery-report**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Este documento consolida todas as descobertas do Estágio 1.
> Preencha cada seção com as conclusões do time. **Este é o input principal do Estágio 2** — sem ele, a especificação vira chute.

**Time**: Workshop Dourado-02 (single-operator)
**Data**: 19/05/2026
**Edição**: 1
**Participantes**: Operador único cobrindo as 10 personas (Pares 1-5)

---

## 1. Sumário Executivo

SIFAP é o sistema legado de pagamento de programas sociais do governo federal, implementado em **Natural/Adabas desde 1996** (29 anos). Compreende **15 programas `.NSN`** (online + batch noturno + relatórios) e **4 DDMs Adabas** (`BENEFICIARIO`, `PROGRAMA-SOCIAL`, `PAGAMENTO`, `AUDITORIA`), processa ~180M pagamentos/mês para ~45 programas sociais ativos. A leitura forense extraiu **30 regras de negócio críticas** e **10 mistérios oficiais** — incluindo bypasses de segurança ainda ativos (CPF, região 99), número mágico `0.347215` sem origem, truncamento monetário sistemático e ocultação de eventos de exclusão nos relatórios de auditoria. O código é funcional mas opaco: documentação operacional não cobre fórmulas de cálculo nem exceções de negócio. A modernização precisa preservar a fórmula central de pagamento e a trilha imutável de auditoria, ao mesmo tempo em que **explicita** as decisões hoje implícitas no código.

---

## 2. Visão Geral do Sistema

### 2.1 Propósito do SIFAP

Sistema central de **cadastro de beneficiários**, **definição de programas sociais**, **geração mensal de ciclo de pagamento**, **conciliação bancária CNAB 240** e **trilha imutável de auditoria** para fiscalização TCU/CGU. Integra com SIAFI (tesouro nacional) e FEBRABAN (rede bancária). Base regulatória: IN-TCU 63/2010, Lei 8159/1991 (arquivos públicos), Decisão CGTI 213/2010 (auditoria).

### 2.2 Arquitetura Legada

- **3 programas online** (`CADBENEF`, `CADDEPEND`, `CADPROG`, `CONSBENF`) operados via terminal 3270.
- **3 batches noturnos** (`BATCHPGT` geração mensal, `BATCHCON` conciliação diária, `BATCHREL` relatórios).
- **6 subprogramas** invocados via `CALLNAT` (`VALBENEF`, `VALDOCS`, `VALELEG`, `CALCBENF`, `CALCCORR`, `CALCDSCT`).
- **2 relatórios** sob demanda (`RELPGT`, `RELAUDIT`).
- **4 DDMs** com uso intensivo de grupos periódicos (`PE`) e multi-value (`MU`); arquivo AUDITORIA INSERT-ONLY desde 1998 acumula ~25M registros.
- **Sem dependências circulares**. Acoplamento concentrado em `BATCHPGT` (chama 3 subprogramas, escreve em 3 DDMs).

Detalhamento: [`dependency-map.md`](dependency-map.md).

### 2.3 Usuários e Perfis

Modelados no campo `AUDITORIA.EC`:

- **ADM** — Administrador (parametriza programas em CADPROG)
- **OPR** — Operador (cadastros CAD*)
- **CON** — Consulta (somente leitura via CONSBENF)
- **AUD** — Auditor (acessa RELAUDIT)
- **SUP** — Supervisor

Volume estimado: ~3.000 operadores ativos distribuídos em 27 estados.

---

## 3. Principais Descobertas

### 3.1 Regras de Negócio Críticas

1. **BR-001 — Fórmula principal de pagamento**: `bruto = BASE × FAT_REG × FAT_FAM × FAT_RND × FAT_IDADE` (cinco fatores multiplicativos). Núcleo financeiro. Ver `business-rules-catalog.md`.
2. **BR-003 — Região 99 bypassa elegibilidade**: `IF REGIAO = 99 ⇒ ELEGIVEL='S'` ignora renda, idade, documentação. Risco regulatório.
3. **BR-012 — CPFs com prefixo especial bypassam validação MOD 11**: lista de 8 prefixos auto-valida. Risco de fraude.
4. **BR-010 — Regime de dezembro**: 13º + abono natalino (+15% para programas tipo 'A') usando fórmula divergente. Fácil de quebrar em refactor.
5. **BR-021 — Conciliação CNAB 240 com tolerância R$ 0,01**: divergências disparam ACAO='DV' em AUDITORIA; estornos podem entrar em loop infinito (BR-022).

### 3.2 Dependências Complexas

- **`BATCHPGT.NSN`** é o ponto de maior acoplamento: chama 3 subprogramas (`VALELEG`, `CALCBENF`, `CALCDSCT`), lê 2 DDMs (`BENEFICIARIO`, `PROGRAMA-SOCIAL`), escreve em 2 (`PAGAMENTO`, `AUDITORIA`). Qualquer mudança em fatores de cálculo cascateia.
- **Cadeia `VALELEG → VALDOCS`** carrega os dois maiores bypasses (BR-003 região 99 e BR-012 CPF prefixo). Devem ser auditados juntos.
- **`CALCBENF → CALCCORR`**: dependência de tabelas IPCA hardcoded/congeladas, tratada como dívida técnica de parametrização.
- **Sem dependências circulares**. Diagrama completo em [`dependency-map.md`](dependency-map.md).

### 3.3 Dívida Técnica Identificada

- [x] **Tabelas hardcoded sem manutenção**: regiões (27 + 3 reservadas, BATCHPGT#L186-L214), IPCA (só 2010-2012, CALCCORR#L18-L67), faixas de renda em R$ correntes não atualizados desde 2005 (BATCHPGT#L229-L234), faixas de contribuição social (CALCDSCT#L39-L48).
- [x] **Campos fantasma e semântica ambígua**: `DOCUMENTOS-OK` referenciado mas inexistente no DDM (INC-002); status `S` usado tanto como suspensão quanto como classificação para idosos >75 (MYS-001).
- [x] **Arredondamento inconsistente**: `BATCHREL` arredonda enquanto `CALCBENF` trunca (MYS-005/INC-001) — relatórios não batem com PAGAMENTO.
- [x] **Auditoria não purga desde 1998**: ~25M registros, custo de storage + violação prazo Lei 8159/1991.
- [x] **Ação EX ocultada em relatório**: exclusões existem na auditoria, mas `RELAUDIT` filtra sem exibir (MYS-010).
- [x] **CPF impresso sem máscara em relatórios** (`RELAUDIT.NSN#L156-L168`): violação LGPD.
- [x] **Bug de máscara CPF congelado por processo** (`CONSBENF.NSN#L97-L108`): mudança bloqueada desde 2014.
- [x] **Loop potencial de estorno** (BATCHCON): sem limite de tentativas.

### 3.4 Gaps de Documentação

- **Não existe** especificação da fórmula `BR-001` fora do código.
- **`FATOR-K`** (PROGRAMA-SOCIAL.BG) sem qualquer documentação — inserido em Ago/2008 com comentário lacônico "ATENDE SOLICITACAO SENARC".
- **Constante `0.347215`** (MYS-003) sem origem identificada.
- **Casos especiais "região 99" e "CPF prefixo"** sem registro do escopo de uso.
- **Lista de easter eggs** (Plano Verão, documentos especiais, Banco Real) só descoberta por leitura forense.

---

## 4. Mistérios e Riscos

### 4.1 Mistérios Não Resolvidos

| ID  | Descrição | Risco para Migração |
| --- | --------- | ------------------- |
| MYS-001 | Status `S` aplicado automaticamente a beneficiários >75 anos | Migração precisa separar situação operacional de classificação demográfica |
| MYS-002 | Dependentes: código limita 5, DDM permite 10 | Risco de perder posições 6-10 em dados legados |
| MYS-003 | Constante mágica `0.347215` em CADPROG | Migrar literal = preservar fator opaco; migrar sem = mudar valor de benefícios |
| MYS-004 | Dezembro usa fórmula própria com 13º e abono | Cálculo mensal genérico subpaga a folha de dezembro |
| MYS-005 | Truncamento monetário sistemático | Arredondamento moderno pode divergir do legado centavo a centavo |
| MYS-006 | Desconto judicial sem teto de 30% | Passivo jurídico se a exceção não for explicitada |
| MYS-007 | CPF prefix bypass | Decisão: manter (fraude) vs remover (quebrar cadastros legados) |
| MYS-008 | Região 99 bypassa elegibilidade | Auditoria regulatória pode invalidar; decisão de negócio obrigatória |
| MYS-009 | Batch depende da ordem por CPF | Reordenar processamento pode quebrar sistemas downstream |
| MYS-010 | Eventos `EX` ocultados em relatório de auditoria | Exclusões podem ficar invisíveis para compliance |

Detalhamento completo: [`mysteries-found.md`](mysteries-found.md).

### 4.2 Riscos para o Estágio 2

1. **Não escreva EARS para regras financeiras sem validar a fórmula BR-001 + os fatores BR-002-006 com SENARC.** Se o time inventar "FAT_RND" diferente do legado, milhões de pagamentos sairão errados.
2. **Bypasses (BR-003, BR-012)** são decisão de negócio + jurídico, não de engenharia. Escalar antes de especificar.
3. **Trilha de auditoria (BR-025)** é **mandatória legal** (IN-TCU 63/2010). Qualquer arquitetura nova precisa garantir INSERT-ONLY + retenção 10 anos e relatórios que não ocultem exclusões (corrigindo MYS-010).
4. **Conciliação CNAB 240 (BR-021/022)** precisa de bounded context próprio com idempotência e limite de retry — comportamento legado tem loop potencial.
5. **Mistérios MYS-003, MYS-006, MYS-007 e MYS-008** são **bloqueadores**: sem resposta do negócio/jurídico, não dá para especificar Cálculo, Documentos e Elegibilidade.

---

## 5. Recomendações

### 5.1 O que migrar primeiro

| Prioridade | Funcionalidade | Justificativa |
| ---------- | -------------- | ------------- |
| 1 | **Auditoria + LGPD compliance** (RELAUDIT, AUDITORIA) | Maior risco regulatório imediato; permite migrar resto com rastreabilidade |
| 2 | **Cadastro de Beneficiário** (CADBENEF, CADDEPEND, CONSBENF) | Entidade central; resolve MYS-001 (status >75) e MYS-002 (5 vs 10 deps) |
| 3 | **Cadastro de Programa Social** (CADPROG) | Pequeno mas crítico; expõe `0.347215` (MYS-003) e o gap documental de `FATOR-K` |
| 4 | **Cálculo de Benefício** (CALCBENF, CALCCORR, CALCDSCT, VALELEG) | Núcleo financeiro; precisa de testes paralelos exaustivos vs legado |
| 5 | **Geração de Ciclo** (BATCHPGT) | Maior volume; depende dos anteriores |
| 6 | **Conciliação Bancária** (BATCHCON) | Pode rodar em paralelo com legado por meses |
| 7 | **Relatórios** (BATCHREL, RELPGT, RELAUDIT) | Camada de apresentação; última prioridade |

**Bounded contexts sugeridos para Estágio 2**:

1. **Cadastro** (Beneficiário + Dependente + Programa Social)
2. **Cálculo de Benefício** (regras BR-001 a BR-019)
3. **Ciclo de Pagamento** (orquestração mensal)
4. **Conciliação Bancária** (CNAB 240 + SIAFI)
5. **Auditoria & Compliance** (trilha imutável + LGPD)
6. **Relatórios** (read-models projetados)

### 5.2 O que descartar

- **Easter egg do Plano Verão** (CALCCORR.NSN#L70-L73): código comentado de 1989-1991, sem valor operacional.
- **Slots reservados de regiões 26-27** (BATCHPGT#L210): tratar como 27 estados fixos no novo modelo.
- **Tipo de desconto 'C'** (CALCDSCT.NSN#L10): declarado nunca usado — remover.
- **CPF prefix bypass (BR-012)** se SENARC confirmar que não há beneficiários ativos com esses prefixos. Caso contrário, transformar em "lista de exceções controladas" explícita e auditada.

### 5.3 O que evoluir

- **Auditoria** (BR-025/026/027): voltar a logar consultas (LGPD), mascarar CPF em todos relatórios, definir política de retenção 10 anos com purge automatizado.
- **Tabelas hardcoded** (regiões, IPCA, faixas de renda, contribuição): externalizar para tabelas de parâmetros versionadas com UI de manutenção e auditoria.
- **Truncamento/arredondamento** (MYS-005/INC-001): definir política financeira e gerar relatório de reconciliação automática.
- **Estorno** (BR-022): adicionar limite de retry + dead-letter queue.
- **`FATOR-K`** (gap documental): documentar a fórmula com SENARC e dar nome semântico; manter parametrização.
- **Limites de descontos** (MYS-006): teto explícito mesmo para judicial OU log de "valor descartado por teto".
- **Status do beneficiário** (MYS-001): separar `STATUS-OPERACIONAL` de `CLASSIFICACAO-ETARIA`.

---

## 6. Métricas do Estágio

| Métrica                       | Valor        |
| ----------------------------- | ------------ |
| Programas analisados          | 15 / 15      |
| DDMs mapeados                 | 4 / 4        |
| Regras de negócio encontradas | 30 catalogadas (de ~116 candidatas) |
| Regras escondidas encontradas | 10 / 10      |
| Easter eggs encontrados       | 3 / 3        |
| Termos no glossário           | 65           |
| Mistérios catalogados         | 10           |
| Tempo total gasto             | ~3 horas (single-operator + subagents paralelos) |

---

## 7. Notas para o Próximo Estágio

**Para a Engenheira de Requisitos (Persona 02) e Arquitetos (03/04):**

1. **Bloqueadores de spec** que exigem resposta do negócio ANTES de escrever EARS:
   - Origem e legitimidade da constante `0.347215` (MYS-003)
   - Escopo de uso da "região 99" (MYS-008)
   - Lista de CPFs reais com prefixo bypass (MYS-007)
   - Regra jurídica para desconto judicial sem teto (MYS-006)
   - Definição de `FATOR-K` (gap documental relacionado ao cálculo)

2. **Para cada EARS**, lembre que `source_legacy:` é **obrigatório**:
   - Use entradas deste relatório como ponte: `source_legacy: 01-arqueologia/legado-sifap/natural-programs/<FILE>.NSN#L<a>-L<b> (ver BR-XXX em business-rules-catalog.md)`
   - Greenfield só com justificativa explícita (ex: "endpoint REST equivalente à tela 3270 — UI moderna, sem correspondente legado").

3. **Prioridade de bounded contexts**: comece por **Auditoria** (risco LGPD) e **Cadastro** (entidades centrais). **Cálculo de Benefício** é o mais complexo — reserve mais tempo.

4. **Estratégia de testes paralelos**: para qualquer regra de cálculo migrada, gere um shadow-run comparando resultado legado vs novo por pelo menos 3 ciclos mensais antes do cutover.

5. **CI/CD `legacy-traceability`**: vai rejeitar PRs sem `source_legacy:`. Use este relatório como índice mestre.

---

## Definição de Pronto deste relatório

- [x] Todas as seções acima preenchidas (sem placeholders).
- [x] Pelo menos 5 regras críticas listadas em §3.1, cada uma referenciando uma `BR-XXX` do catálogo.
- [x] Decisões de migrar/descartar/evoluir em §5 cobrem as 8+ funcionalidades principais.
- [x] Métricas de §6 conferem com os outros artefatos (glossary.md: 65 termos, business-rules-catalog.md: 30 regras, mysteries-found.md: 10 mistérios).

— Workshop Dourado-02


---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="mysteries-found.md"><strong>mysteries-found.md</strong></a><br/>
<sub>Lista de mistérios.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="../02-spec-moderna/GUIDE.md"><strong>Estágio 2 — Spec</strong></a><br/>
<sub>Próximo estágio: spec moderna.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>

