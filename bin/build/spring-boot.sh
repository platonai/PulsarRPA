#!/usr/bin/env bash

# Find the first parent directory that contains a VERSION file
BIN=$(dirname "$0")
APP_HOME=$(realpath "$BIN/..")
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] || exit 1

MVNW="$APP_HOME"/mvnw

"$BIN"/build.sh "$@"

SERVER_HOME=$APP_HOME/browser4/browser4-agents
cd "$SERVER_HOME" || exit

"$BIN"/tools/install-depends.sh
"$MVNW" spring-boot:run

cd "$APP_HOME" || exit
