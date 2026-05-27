#!/usr/bin/env bash
# Commit organizado do código SIFAP 2.0
set -euo pipefail

REPO="/mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02"
cd "$REPO"

echo "=== Criando branch impl/sifap-modernizacao ==="
git checkout -b impl/sifap-modernizacao 2>/dev/null || git checkout impl/sifap-modernizacao

# ── 1. CI/CD corrigido ───────────────────────────────────────────────────────
echo ""
echo "--- [1/6] CI/CD ---"
git add .github/workflows/ci.yml
git commit -m "ci: corrige paths backend/frontend e adiciona job traceability

- backend: prototype/backend/** (era backend/**)
- frontend: prototype/frontend/** (era frontend/**)
- infra: infra/** (novo)
- specs: specs/** com job traceability dedicado
- remove job legacy-traceability duplicado (já coberto)

Refs: ADR-003"

# ── 2. Backend Dockerfile ────────────────────────────────────────────────────
echo ""
echo "--- [2/6] Backend Dockerfile ---"
git add prototype/backend/Dockerfile
git commit -m "build(backend): adiciona Dockerfile multi-stage Java 21

- Stage build: eclipse-temurin:21-jdk-alpine + Maven
- Stage runtime: eclipse-temurin:21-jre-alpine, usuário não-root
- JVM flags: UseContainerSupport + MaxRAMPercentage=75

Refs: ADR-001 (Modular Monolith)"

# ── 3. Frontend Next.js 15 ───────────────────────────────────────────────────
echo ""
echo "--- [3/6] Frontend Next.js 15 ---"
git add prototype/frontend/
git commit -m "feat(frontend): cria frontend Next.js 15 + TypeScript + Tailwind

Componentes:
- PaymentCyclePanel: POST /api/v1/payment-cycles (REQ-PAY-001)
- HealthBadge: status do backend via /actuator/health
- page.tsx: dashboard com rastreabilidade legado visível

Testes (Vitest + Testing Library):
- 4 cenários: sucesso 201, CYCLE_TOO_EARLY 422, CYCLE_ALREADY_EXISTS 409, NETWORK_ERROR

Stack: Next.js 15.5.18, TypeScript strict, Tailwind CSS, pnpm 9
Dockerfile: multi-stage node:20-alpine, output standalone

source_legacy: BATCHPGT.NSN#L42-L78 (REQ-PAY-001 / BR-007)"

# ── 4. Terraform Azure ───────────────────────────────────────────────────────
echo ""
echo "--- [4/6] Terraform Azure ---"
git add infra/
git commit -m "feat(infra): Terraform Azure — Container Apps + PostgreSQL 16 + Key Vault

Recursos:
- azurerm_resource_group
- azurerm_log_analytics_workspace
- azurerm_container_app_environment
- azurerm_postgresql_flexible_server (v16)
- azurerm_key_vault + secrets (pg-password, jwt-secret)
- azurerm_container_app: backend (porta 8080) + frontend (porta 3000)

Regras ADR:
- tags obrigatórias: project, environment, owner, managed_by
- secrets somente via Key Vault (nunca em locals/variables)
- um módulo por área de serviço

terraform validate: ✅ Success! The configuration is valid."

# ── 5. Scripts WSL + relatório ───────────────────────────────────────────────
echo ""
echo "--- [5/6] Scripts WSL + relatório ---"
git add wsl-dev.sh run-backend-test.sh run-frontend-install.sh \
        run-terraform-validate.sh setup-maven.sh setup-node.sh \
        install-tools-wsl.sh check-next-version.sh \
        04-evolucao/agent-experience-report.md \
        docs/issue-REQ-PAY-004-region-exception.md
git commit -m "docs: agent-experience-report + issue REQ-PAY-004 + scripts WSL

- agent-experience-report.md: relatório honesto preenchido
  (2 issues, 2 PRs, notas 1-5, recomendações para outros times)
- issue-REQ-PAY-004-region-exception.md: issue bem escrita para
  Copilot Agent (exceção de região, migração V3, testes)
- wsl-dev.sh: script mestre (setup/test/build/up/down/tf-validate/spec-check/all)
- scripts auxiliares de setup do ambiente WSL Ubuntu"

# ── 6. .gitignore + demais modificados ──────────────────────────────────────
echo ""
echo "--- [6/6] .gitignore + arquivos modificados ---"
git add .gitignore
git add .github/workflows/
git add specs/
git commit -m "chore: remove prototype/ e infra/ do .gitignore

Esses diretórios eram symlinks para repositório de referência externo.
Agora contêm código real do time e devem ser rastreados pelo git.

Também inclui:
- specs/001-ciclo-pagamento/spec.md: 14 REQ-IDs EARS com source_legacy
- .github/workflows/: ci.yml corrigido + legacy-traceability.yml"

echo ""
echo "=== Status final ==="
git log --oneline -6
echo ""
echo "✅ Commits criados na branch impl/sifap-modernizacao"
echo "   Para push: git push -u origin impl/sifap-modernizacao"
