#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# SIFAP Workshop — Instalação de ferramentas no WSL Ubuntu
# Execute: bash install-tools-wsl.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║   SIFAP Workshop — Setup de ferramentas (WSL Ubuntu)    ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# ── 1. Node.js 20 LTS ────────────────────────────────────────────────────────
echo "▶ [1/4] Instalando Node.js 20 LTS..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
echo "   Node: $(node --version)  npm: $(npm --version)"

# ── 2. pnpm ──────────────────────────────────────────────────────────────────
echo ""
echo "▶ [2/4] Instalando pnpm..."
sudo npm install -g pnpm@9
echo "   pnpm: $(pnpm --version)"

# ── 3. Maven 3.9 ─────────────────────────────────────────────────────────────
echo ""
echo "▶ [3/4] Instalando Maven 3.9.9..."
MVN_VERSION="3.9.9"
MVN_URL="https://downloads.apache.org/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz"
cd /tmp
curl -fsSL "$MVN_URL" -o maven.tar.gz
sudo tar -xzf maven.tar.gz -C /opt
sudo ln -sf /opt/apache-maven-${MVN_VERSION}/bin/mvn /usr/local/bin/mvn
rm maven.tar.gz
echo "   Maven: $(mvn -version 2>&1 | head -1)"

# ── 4. Docker Compose plugin (v2) ────────────────────────────────────────────
echo ""
echo "▶ [4/4] Instalando Docker Compose plugin v2..."
COMPOSE_VERSION="v2.29.7"
COMPOSE_DIR="${HOME}/.docker/cli-plugins"
mkdir -p "$COMPOSE_DIR"
curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
     -o "${COMPOSE_DIR}/docker-compose"
chmod +x "${COMPOSE_DIR}/docker-compose"
echo "   docker compose: $(docker compose version)"

# ── Resumo ───────────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║                    ✅  Tudo instalado!                   ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "  Node.js : $(node --version)"
echo "  npm     : $(npm --version)"
echo "  pnpm    : $(pnpm --version)"
echo "  Maven   : $(mvn -version 2>&1 | head -1)"
echo "  Docker  : $(docker --version)"
echo "  Compose : $(docker compose version)"
echo "  Terraform: $(terraform --version | head -1)"
echo ""
echo "Próximo passo: volte ao Kiro e continue o build do SIFAP."
