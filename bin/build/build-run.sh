#!/usr/bin/env bash

# Find the first parent directory that contains a VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit 1; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit 1

"$APP_HOME"/bin/build/build.sh "$@"

"$APP_HOME"/bin/tools/install-depends.sh

"$APP_HOME"/bin/browser4.sh "$@"
