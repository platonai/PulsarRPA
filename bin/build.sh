#!/bin/bash

BIN=$(dirname "$0")
APP_HOME=$(realpath "$BIN/..")

CLEAN=false
SKIP_TEST=true

while [[ $# -gt 0 ]]; do
  case $1 in
    -c|--clean)
      CLEAN="clean"
      shift # past argument
      ;;
    -t|--test)
      SKIP_TEST=false
      shift # past argument
      ;;
    -*|--*)
      echo "Unknown option $1"
      exit 1
      ;;
  esac
done

MVNW="$APP_HOME/mvnw"
MVN_CALL=()

if [ "$CLEAN" = true ]; then
  MVN_CALL+=("clean")
fi

if [ "$SKIP_TEST" = true ]; then
  MVN_CALL+=("-DskipTests=true")
fi
MVN_CALL+=("-Pall-modules")

cd "$APP_HOME" || exit

"$MVNW" "${MVN_CALL[@]}"
