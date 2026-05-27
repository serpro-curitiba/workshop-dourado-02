#!/usr/bin/env bash
set -euo pipefail
cd /mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02

echo "=== Mudando para main ==="
git checkout main

echo "=== Merge de impl/sifap-modernizacao → main ==="
git merge --no-ff impl/sifap-modernizacao \
  -m "feat: SIFAP 2.0 — modernização completa demonstrável

Inclui todos os artefatos para demo ao vivo:

Backend (Java 21 + Spring Boot 3.3):
- Dockerfile multi-stage (eclipse-temurin:21)
- 4 bounded contexts implementados: pagamento, calculo, elegibilidade, conciliacao
- 20 testes passando (mvn test)
- Flyway V1 + V2 (payment + bank_return_file)

Frontend (Next.js 15.5.18 + TypeScript + Tailwind):
- Dashboard com PaymentCyclePanel (REQ-PAY-001)
- HealthBadge com status do backend
- 4 testes Vitest + Testing Library
- Dockerfile multi-stage (node:20-alpine, output standalone)

Infraestrutura (Terraform Azure):
- Container Apps + PostgreSQL 16 + Key Vault
- terraform validate: ✅ Success
- Tags obrigatórias em todos os recursos (ADR)

CI/CD (GitHub Actions):
- Paths corrigidos: prototype/backend/**, prototype/frontend/**
- Job traceability: valida source_legacy em todos os REQ-IDs
- Jobs: backend, frontend, infra, traceability

Specs EARS:
- specs/001-ciclo-pagamento/spec.md: 14 REQ-IDs com source_legacy
- validate-traceability.py: ✅ todos os specs passaram

Documentação:
- agent-experience-report.md preenchido (Estágio 4)
- issue REQ-PAY-004 bem escrita para Copilot Agent
- wsl-dev.sh: script mestre de desenvolvimento

docker compose up: postgres + backend + frontend prontos"

echo ""
echo "=== Log final ==="
git log --oneline -8

echo ""
echo "=== Deletando branch impl/sifap-modernizacao ==="
git branch -d impl/sifap-modernizacao

echo ""
echo "✅ main atualizado — pronto para push"
echo "   Execute: git push origin main"
