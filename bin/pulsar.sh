#!/usr/bin/env bash

set -e

# Find the first parent directory that contains a pom.xml file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$APP_HOME" != "/" ]]; do
  if [[ -f "$APP_HOME/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$APP_HOME")
done

cd "$APP_HOME" || exit

# find out chrome version
CHROME_VERSION="$(google-chrome -version | head -n1 | awk -F '[. ]' '{print $3}')"
if [[ "$CHROME_VERSION" == "" ]]; then
  echo "Google Chrome is not found in your system, you can run bin/install-depends.sh to do it automatically"
  exit
fi

UBERJAR="$APP_HOME"/target/PulsarRPA.jar
if [ ! -f "$UBERJAR" ]; then
  SERVER_HOME=$APP_HOME/pulsar-app/pulsar-master
  cp "$SERVER_HOME"/target/PulsarRPA.jar "$UBERJAR"
fi

java -jar "$UBERJAR"
