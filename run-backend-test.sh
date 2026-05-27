#!/usr/bin/env bash
set -e
export M2_HOME="$HOME/.local/opt/apache-maven-3.9.16"
export PATH="$M2_HOME/bin:$PATH"
echo "Maven: $(mvn -version 2>&1 | head -1)"
cd /mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02/prototype/backend
mvn -B test
echo "=== BACKEND TESTS OK ==="
