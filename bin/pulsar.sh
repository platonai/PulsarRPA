#!/usr/bin/env bash

set -e

# Find the first parent directory that contains a VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit

# find out chrome version
CHROME_VERSION="$(google-chrome -version | head -n1 | awk -F '[. ]' '{print $3}')"
if [[ "$CHROME_VERSION" == "" ]]; then
  echo "Google Chrome is not found in your system, you can run bin/install-depends.sh to do it automatically"
  exit
fi

UBERJAR="$APP_HOME"/target/PulsarRPA.jar
if [ ! -f "$UBERJAR" ]; then
  SERVER_HOME=$APP_HOME/pulsar-app/pulsar-browser4
  cp "$SERVER_HOME"/target/PulsarRPA.jar "$UBERJAR"
fi

java -jar "$UBERJAR"
