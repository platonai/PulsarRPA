#!/usr/bin/env bash

function __dev_mode_enable_module() {
  M=$1
  shift
  JAR1=$1
  shift

  cd "$APP_HOME/$M" || exit
  outputFile="/tmp/$M.classpath"
  mvn dependency:build-classpath -Dmdep.outputFile="$outputFile"
  cd - || exit

  MODULE_CLASSPATH=$(cat "$outputFile")
  CLASSPATH=${CLASSPATH}:"$MODULE_CLASSPATH"
  CLASSPATH=${CLASSPATH}:"$APP_HOME/$M/target/$JAR1";

  # additional resources
  if [ "$M" == "pulsar-app/pulsar-master" ]; then
    CLASSPATH=${CLASSPATH}:"$APP_HOME/$M"/src/main/resources;
  fi
}
