#!/usr/bin/env bash

BIN=$(dirname "$0")
APP_HOME=$(realpath "$BIN/..")
MVNW="$APP_HOME"/mvnw

"$BIN"/build.sh "$@"

SERVER_HOME=$APP_HOME/pulsar-app/pulsar-master
cd "$SERVER_HOME" || exit

"$BIN"/tools/install-depends.sh
"$MVNW" spring-boot:run

cd "$APP_HOME" || exit
