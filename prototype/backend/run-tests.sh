#!/usr/bin/env bash
set -e
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export M2_HOME="/mnt/c/Users/s006420399/Desenvolvimento/Workspace_SERPRO/apache-maven-3.6.3"
export PATH="$M2_HOME/bin:$JAVA_HOME/bin:/usr/bin:/bin"
cd "/mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02/prototype/backend"
mvn -B -DskipITs test
