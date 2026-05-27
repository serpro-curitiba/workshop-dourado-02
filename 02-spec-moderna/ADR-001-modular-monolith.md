<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# ADR-001: Adotar Modular Monolith (não microserviços) para SIFAP 2.0

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![Status Aceita](https://img.shields.io/badge/STATUS-Aceita-7FBA00?style=for-the-badge)

**Data**: 27/05/2026
**Status**: Aceita
**Decisores**: Enterprise Architect (persona 03), Software Architect (persona 04), Technical Lead (persona 05)

## Contexto

O SIFAP legado é um sistema Natural/Adabas de 29 anos com 15 programas, 4 DDMs, ~180M pagamentos/mês e ~25M registros de auditoria. A modernização precisa preservar regras críticas (BR-001 a BR-030) e responder a riscos regulatórios (LGPD, IN-TCU 63/2010). O time tem 5 pessoas (single-operator cobrindo 10 personas neste workshop), prazo agressivo (workshop de 1 dia, modernização real em 6 meses), e nenhum operador atual tem experiência com microserviços distribuídos.

Restrições adicionais:

- Cutover precisa de **shadow-run paralelo** (legado + novo) por 3 ciclos mensais — exige consistência forte
- Trilha de auditoria (BR-025) é INSERT-ONLY com transação atômica entre escrita de domínio + auditoria
- Conciliação CNAB 240 lê milhões de linhas em janela noturna curta
- Equipe atual conhece Java + Spring; sem expertise em service mesh, sagas distribuídas

## Opções Consideradas

### Opção 1: Microserviços por bounded context (8 serviços)

- **Descrição**: 1 serviço por contexto (cadastro, elegibilidade, cálculo, pagamento, conciliação, auditoria, relatórios, iam), comunicação via REST + eventos Kafka, banco por serviço.
- **Vantagens**: Escala independente; deploy isolado; tecnologia heterogênea; alinhado a "best practice" em conferência.
- **Desvantagens**: Sagas distribuídas para garantir atomicidade domínio+auditoria; latência rede × 8 hops por ciclo; complexidade operacional (8 pipelines CI, 8 dashboards, 8 alertas); time não tem experiência; shadow-run vira coordenação distribuída.

### Opção 2: Monolito tradicional (camadas técnicas)

- **Descrição**: Pacotes por camada (`controller/`, `service/`, `repository/`), tudo em um WAR.
- **Vantagens**: Simples; familiar; CI rápido.
- **Desvantagens**: Acoplamento entre features cresce rápido; difícil extrair serviços depois (Strangler Fig fica bloqueado); reproduz o problema do legado (15 programas Natural sem fronteiras claras).

### Opção 3: Modular Monolith (package-by-feature por bounded context)

- **Descrição**: 1 deployable Spring Boot, mas pacotes Java por bounded context (`br.gov.serpro.sifap.{cadastro,elegibilidade,calculo,...}`). Comunicação entre módulos via interfaces Java (SPI) + eventos Spring `ApplicationEvent` síncronos no início, podendo migrar para Kafka quando necessário. Cada módulo é candidato natural a serviço futuro.
- **Vantagens**: Transações ACID locais (auditoria atômica resolvida); shadow-run trivial; CI/CD único; complexidade operacional baixa; alinha com bounded contexts de [`bounded-contexts.md`](bounded-contexts.md); Strangler Fig direto (extrair `calculo` ou `auditoria` como serviço primeiro quando justificar); compatível com `archunit` para enforcement de fronteiras.
- **Desvantagens**: Não escala módulos independentemente (mitigado: cargas de leitura vão para read-replicas; cálculo pode rodar em pool de threads dedicado/virtual threads); um bug pode derrubar o deploy inteiro (mitigado: graceful degradation por módulo + health endpoint por módulo).

## Decisão

**Decidimos adotar Modular Monolith (Opção 3) para SIFAP 2.0**, com pacote-por-feature seguindo os 8 bounded contexts definidos em [`bounded-contexts.md`](bounded-contexts.md), comunicação interna via interfaces Java e eventos Spring, e regras `archunit` no CI para impedir vazamento entre módulos.

## Justificativa

1. **Atomicidade auditoria + domínio (BR-025)** é crítico legal — única transação JPA garante; sagas adicionariam risco sem benefício.
2. **Shadow-run (N7)** é mais simples sem latência inter-serviço.
3. **Equipe** entrega Modular Monolith em 6 meses; microserviços levariam 18+ meses com retrabalho.
4. **Strangler Fig preservado**: a fronteira lógica entre módulos permite extrair qualquer um como serviço quando justificar (provável candidato: `calculo` para escalar cálculos sob demanda, ou `auditoria` para isolar carga de leitura de auditor).
5. **Bounded contexts já definidos** (8 módulos) viram diretamente pacotes — alinhamento conceitual perfeito.
6. **Operação**: 1 pipeline, 1 dashboard, 1 alerta — operável pelo time atual.

## Consequências

### Positivas

- Atomicidade ACID entre escrita de domínio e auditoria (compliance imediato).
- CI/CD único (uma pipeline GitHub Actions).
- Shadow-run trivial (mesmo processo).
- Onboarding rápido para devs Java.
- `archunit` enforça fronteiras → preserva opção de extrair serviços depois.

### Negativas

- **Não escala módulos independentemente** — mitigar com: (a) read-replicas PostgreSQL para `relatorios` e `auditoria`, (b) pool de threads virtuais Java 21 dedicado para `calculo`, (c) particionamento de tabelas `payment` por competência e `audit_event` por mês.
- **Acoplamento de tecnologia (Java 21 + Spring Boot 3.3)** — aceitável: time conhece, ecossistema maduro.
- **Bug pode afetar todo o sistema** — mitigar com: feature flags por módulo, health endpoint por módulo, circuit breakers em integrações externas.

### Riscos

- **Risco**: módulo `calculo` virar gargalo durante geração mensal. **Plano**: monitorar p95 desde dia 1; se atingir 70% da janela noturna, extrair como serviço com worker pool.
- **Risco**: dev quebrar fronteira entre módulos por atalho. **Plano**: regras `archunit` rodam no CI (PR rejeita); review obrigatório de Pull Request.

## Referências

- [`bounded-contexts.md`](bounded-contexts.md) — os 8 contextos
- [`scope-decisions.md`](scope-decisions.md) — funcionalidades migrar/evoluir/descartar
- [`../01-arqueologia/discovery-report.md`](../01-arqueologia/discovery-report.md) §5 — recomendações
- BR-025 (auditoria atômica), N7 (shadow-run), N10 (health endpoints)
- ["Building Evolutionary Architectures" (Ford, Parsons, Kua)](https://www.thoughtworks.com/insights/blog/microservices/evolutionary-architecture-emergent-design)
- ["Modular Monolith: A Primer" (Kamil Grzybek)](https://www.kamilgrzybek.com/design/modular-monolith-primer/)
