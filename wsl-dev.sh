#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# SIFAP 2.0 — Script mestre de desenvolvimento (WSL Ubuntu)
# Uso: bash wsl-dev.sh [comando]
#
# Comandos:
#   setup       Configura ambiente (Maven, Node, pnpm, Docker Compose)
#   test        Roda todos os testes (backend + frontend)
#   build       Compila backend e frontend
#   up          Sobe stack completa via docker-compose
#   down        Para a stack
#   tf-validate Valida Terraform (sem credenciais Azure)
#   spec-check  Valida rastreabilidade source_legacy nos specs
#   all         setup + test + tf-validate + spec-check
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

REPO_ROOT="/mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02"
LINUX_FRONTEND="$HOME/sifap-frontend"
MVN_HOME="$HOME/.local/opt/apache-maven-3.9.16"
NVM_DIR="$HOME/.nvm"

# Monta drive C se automount estiver desabilitado
if [ ! -d /mnt/c ]; then
  sudo mkdir -p /mnt/c
  sudo mount -t drvfs C: /mnt/c -o metadata,uid=$(id -u),gid=$(id -g)
fi

# ── Helpers ──────────────────────────────────────────────────────────────────
load_tools() {
  export M2_HOME="$MVN_HOME"
  export PATH="$M2_HOME/bin:$PATH"
  [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" && nvm use 20 --silent
  export PATH="$NVM_DIR/versions/node/v20.20.2/bin:$PATH"
}

ok()   { echo -e "\033[32m✅ $*\033[0m"; }
fail() { echo -e "\033[31m❌ $*\033[0m"; exit 1; }
info() { echo -e "\033[34mℹ  $*\033[0m"; }

# ── Comandos ─────────────────────────────────────────────────────────────────
cmd_setup() {
  info "Verificando ferramentas..."
  load_tools
  echo "  Maven   : $(mvn -version 2>&1 | head -1)"
  echo "  Node    : $(node --version)"
  echo "  pnpm    : $(pnpm --version)"
  echo "  Docker  : $(docker --version)"
  echo "  Compose : $(docker compose version)"
  echo "  Terraform: $(terraform --version | head -1)"
  ok "Ambiente OK"
}

cmd_test_backend() {
  info "Rodando testes do backend..."
  load_tools
  cd "$REPO_ROOT/prototype/backend"
  mvn -B test -q
  ok "Backend: $(grep -c 'Tests run' target/surefire-reports/*.txt 2>/dev/null || echo '?') suites passaram"
}

cmd_test_frontend() {
  info "Rodando testes do frontend..."
  load_tools
  # opera em filesystem Linux para evitar EACCES no /mnt/c
  rm -rf "$LINUX_FRONTEND"
  cp -r "$REPO_ROOT/prototype/frontend" "$LINUX_FRONTEND"
  cd "$LINUX_FRONTEND"
  [ -f pnpm-lock.yaml ] || pnpm install
  pnpm install --frozen-lockfile 2>/dev/null || pnpm install
  pnpm typecheck
  pnpm test:run
  # devolve lock file atualizado
  cp pnpm-lock.yaml "$REPO_ROOT/prototype/frontend/pnpm-lock.yaml"
  ok "Frontend: testes passaram"
}

cmd_test() {
  cmd_test_backend
  cmd_test_frontend
}

cmd_build_backend() {
  info "Compilando backend (sem testes)..."
  load_tools
  cd "$REPO_ROOT/prototype/backend"
  mvn -B package -DskipTests -q
  ok "Backend JAR gerado em target/"
}

cmd_up() {
  info "Subindo stack via docker-compose..."
  export DOCKER_BUILDKIT=1
  cd "$REPO_ROOT"
  docker compose up -d --build
  info "Aguardando healthchecks..."
  sleep 10
  docker-compose ps
  ok "Stack rodando — frontend: http://localhost:3001  backend: http://localhost:8080"
}

cmd_down() {
  info "Parando stack..."
  cd "$REPO_ROOT"
  docker-compose down
  ok "Stack parada"
}

cmd_tf_validate() {
  info "Validando Terraform..."
  cd "$REPO_ROOT/infra"
  terraform fmt -check -recursive || terraform fmt -recursive
  terraform init -backend=false -input=false -no-color 2>&1 | tail -3
  terraform validate
  ok "Terraform: configuração válida"
}

cmd_spec_check() {
  info "Validando rastreabilidade source_legacy..."
  cd "$REPO_ROOT"
  python3 scripts/validate-traceability.py
  ok "Specs: rastreabilidade OK"
}

cmd_all() {
  cmd_setup
  cmd_test_backend
  cmd_test_frontend
  cmd_tf_validate
  cmd_spec_check
  echo ""
  ok "=== TUDO VERDE — pronto para demo ==="
}

# ── Dispatcher ───────────────────────────────────────────────────────────────
CMD="${1:-help}"
case "$CMD" in
  setup)        cmd_setup ;;
  test)         cmd_test ;;
  test-backend) cmd_test_backend ;;
  test-frontend)cmd_test_frontend ;;
  build)        cmd_build_backend ;;
  up)           cmd_up ;;
  down)         cmd_down ;;
  tf-validate)  cmd_tf_validate ;;
  spec-check)   cmd_spec_check ;;
  all)          cmd_all ;;
  *)
    echo "Uso: bash wsl-dev.sh [setup|test|test-backend|test-frontend|build|up|down|tf-validate|spec-check|all]"
    ;;
esac
