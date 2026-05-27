#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# SIFAP 2.0 — Script mestre de desenvolvimento (WSL Ubuntu)
# Uso: bash wsl-dev.sh [comando]
#
# Comandos:
#   setup       Configura ambiente (Maven, Node, pnpm, Docker Compose)
#   test        Roda todos os testes (backend + frontend)
#   build       Compila backend e frontend
#   recycle-frontend Rebuilda e recria somente o frontend publicado
#   verify-ui   Confirma que a pagina publicada contem as abas esperadas
#   up          Sobe stack completa via Docker Compose
#   down        Para a stack
#   tf-validate Valida Terraform (sem credenciais Azure)
#   spec-check  Valida rastreabilidade source_legacy nos specs
#   all         setup + test + tf-validate + spec-check
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

REPO_ROOT="/mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"
FRONTEND_SRC="$REPO_ROOT/prototype/frontend"
LINUX_FRONTEND="$HOME/sifap-frontend"
MVN_HOME="$HOME/.local/opt/apache-maven-3.9.16"
NVM_DIR="$HOME/.nvm"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3001}"

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

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "$COMPOSE_FILE" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "$COMPOSE_FILE" "$@"
  else
    fail "Docker Compose nao encontrado"
  fi
}

compose_version() {
  if docker compose version >/dev/null 2>&1; then
    docker compose version
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose version
  else
    echo "indisponivel"
  fi
}

sync_frontend_to_linux() {
  command -v rsync >/dev/null 2>&1 || fail "rsync nao encontrado no WSL"
  rm -rf "$LINUX_FRONTEND"
  mkdir -p "$LINUX_FRONTEND"
  rsync -a --delete \
    --exclude node_modules \
    --exclude .next \
    "$FRONTEND_SRC/" "$LINUX_FRONTEND/"
}

wait_for_healthy() {
  local container="$1"
  local timeout_seconds="${2:-120}"
  local start
  start="$(date +%s)"

  while true; do
    local status
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    case "$status" in
      healthy|running) return 0 ;;
      unhealthy|exited|dead) fail "$container ficou em estado '$status'" ;;
    esac

    if [ "$(( $(date +%s) - start ))" -ge "$timeout_seconds" ]; then
      fail "Timeout aguardando $container ficar healthy"
    fi
    sleep 2
  done
}

remove_compose_zombies() {
  local container_name
  for container_name in "$@"; do
    docker ps -aq --filter "name=_${container_name}" | xargs -r docker rm -f >/dev/null
  done
}

remove_named_containers() {
  docker rm -f "$@" >/dev/null 2>&1 || true
  remove_compose_zombies "$@"
}

# ── Comandos ─────────────────────────────────────────────────────────────────
cmd_setup() {
  info "Verificando ferramentas..."
  load_tools
  echo "  Maven   : $(mvn -version 2>&1 | head -1)"
  echo "  Node    : $(node --version)"
  echo "  pnpm    : $(pnpm --version)"
  echo "  Docker  : $(docker --version)"
  echo "  Compose : $(compose_version)"
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
  # Opera em filesystem Linux para evitar EACCES e binarios Windows em /mnt/c.
  # Nao copie node_modules/.next: isso deixa o processo lento e pode travar.
  sync_frontend_to_linux
  cd "$LINUX_FRONTEND"
  if [ -f pnpm-lock.yaml ]; then
    pnpm install --frozen-lockfile 2>/dev/null || pnpm install
  else
    pnpm install --no-frozen-lockfile
  fi
  pnpm typecheck
  pnpm test:run
  [ -f pnpm-lock.yaml ] && cp pnpm-lock.yaml "$FRONTEND_SRC/pnpm-lock.yaml"
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

cmd_build_frontend() {
  info "Compilando frontend em filesystem Linux..."
  load_tools
  sync_frontend_to_linux
  cd "$LINUX_FRONTEND"
  if [ -f pnpm-lock.yaml ]; then
    pnpm install --frozen-lockfile 2>/dev/null || pnpm install
  else
    pnpm install --no-frozen-lockfile
  fi
  pnpm build
  [ -f pnpm-lock.yaml ] && cp pnpm-lock.yaml "$FRONTEND_SRC/pnpm-lock.yaml"
  ok "Frontend: build passou"
}

cmd_build() {
  cmd_build_backend
  cmd_build_frontend
}

cmd_verify_ui() {
  info "Validando pagina publicada em $FRONTEND_URL..."
  local page
  page="$(curl -fsS "$FRONTEND_URL/")"
  for label in Abertura Pagamentos CNAB Conciliação IPCA Auditoria; do
    printf "%s" "$page" | grep -Fq "$label" || fail "Aba '$label' nao encontrada em $FRONTEND_URL"
  done
  ok "UI publicada contem as abas esperadas"
}

cmd_recycle_frontend() {
  info "Rebuildando imagem production do frontend..."
  export DOCKER_BUILDKIT=1
  cd "$REPO_ROOT"
  compose build frontend

  info "Recriando somente o frontend para publicar alteracoes locais..."
  # O docker-compose v1 pode falhar com KeyError: ContainerConfig ao recriar
  # containers antigos. Remover o container antes evita esse caminho quebrado.
  remove_named_containers sifap-frontend
  compose up -d --no-deps frontend
  wait_for_healthy sifap-frontend 120
  cmd_verify_ui
  ok "Frontend reciclado — abra $FRONTEND_URL e use Ctrl+F5 se o navegador mantiver cache"
}

cmd_up() {
  info "Subindo stack via Docker Compose..."
  export DOCKER_BUILDKIT=1
  cd "$REPO_ROOT"
  compose build
  # Evita bug do docker-compose v1 em recriacoes e garante que a imagem nova
  # do frontend seja efetivamente usada. Volumes nomeados, como pgdata, ficam.
  remove_named_containers sifap-frontend sifap-backend sifap-postgres
  compose up -d
  wait_for_healthy sifap-postgres 120
  wait_for_healthy sifap-backend 180
  wait_for_healthy sifap-frontend 180
  compose ps
  cmd_verify_ui
  ok "Stack rodando — frontend: http://localhost:3001  backend: http://localhost:8080"
}

cmd_down() {
  info "Parando stack..."
  cd "$REPO_ROOT"
  compose down
  remove_compose_zombies sifap-frontend sifap-backend sifap-postgres
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
  cmd_build_frontend
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
  build)        cmd_build ;;
  build-backend)cmd_build_backend ;;
  build-frontend)cmd_build_frontend ;;
  recycle-frontend)cmd_recycle_frontend ;;
  verify-ui)    cmd_verify_ui ;;
  up)           cmd_up ;;
  down)         cmd_down ;;
  tf-validate)  cmd_tf_validate ;;
  spec-check)   cmd_spec_check ;;
  all)          cmd_all ;;
  *)
    echo "Uso: bash wsl-dev.sh [setup|test|test-backend|test-frontend|build|build-backend|build-frontend|recycle-frontend|verify-ui|up|down|tf-validate|spec-check|all]"
    ;;
esac
