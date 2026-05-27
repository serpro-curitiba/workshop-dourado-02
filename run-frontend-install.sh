#!/usr/bin/env bash
# Instala dependências do frontend em filesystem Linux nativo (evita EACCES no /mnt/c)
# e roda typecheck + testes. Copia pnpm-lock.yaml de volta ao projeto.
set -euo pipefail

source "$HOME/.nvm/nvm.sh"
nvm use 20 --silent
export PATH="$HOME/.nvm/versions/node/v20.20.2/bin:$PATH"
echo "Node: $(node --version)  pnpm: $(pnpm --version)"

WIN_SRC="/mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02/prototype/frontend"
LINUX_DIR="$HOME/sifap-frontend"

echo "=== Copiando frontend para $LINUX_DIR ==="
rm -rf "$LINUX_DIR"
cp -r "$WIN_SRC" "$LINUX_DIR"
cd "$LINUX_DIR"

echo "=== pnpm install ==="
pnpm install --no-frozen-lockfile

echo "=== typecheck ==="
pnpm typecheck

echo "=== testes ==="
pnpm test:run

echo "=== Copiando pnpm-lock.yaml de volta ==="
cp pnpm-lock.yaml "$WIN_SRC/pnpm-lock.yaml"

echo ""
echo "✅ FRONTEND OK"
