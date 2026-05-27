#!/usr/bin/env bash
set -e
MVN_VERSION="3.9.16"
URL="https://dlcdn.apache.org/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz"
echo "Baixando Maven ${MVN_VERSION}..."
curl -fsSL "$URL" -o /tmp/maven.tar.gz
mkdir -p "$HOME/.local/opt"
tar -xzf /tmp/maven.tar.gz -C "$HOME/.local/opt"
rm /tmp/maven.tar.gz
MVN_HOME="$HOME/.local/opt/apache-maven-${MVN_VERSION}"
for RC in "$HOME/.bashrc" "$HOME/.profile"; do
  grep -q "apache-maven" "$RC" 2>/dev/null && sed -i '/apache-maven/d' "$RC" || true
  echo "export M2_HOME=${MVN_HOME}" >> "$RC"
  echo "export PATH=\$M2_HOME/bin:\$PATH" >> "$RC"
done
export M2_HOME="$MVN_HOME"
export PATH="$M2_HOME/bin:$PATH"
echo "Maven: $(mvn -version 2>&1 | head -1)"
