#!/usr/bin/env bash
set -e
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
nvm install 20
nvm use 20
nvm alias default 20
echo "Node: $(node --version)"
echo "npm:  $(npm --version)"
npm install -g pnpm@9
echo "pnpm: $(pnpm --version)"
