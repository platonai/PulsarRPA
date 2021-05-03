#!/usr/bin/env bash

function __dev_mode_enable_module() {
  M=$1
  shift
  JAR1=$1
  shift

  cd "$PULSAR_HOME/$M" || exit
  outputFile="/tmp/$M.classpath"
  mvn dependency:build-classpath -Dmdep.outputFile="$outputFile"
  cd - || exit

  MODULE_CLASSPATH=$(cat "$outputFile")
  CLASSPATH=${CLASSPATH}:"$MODULE_CLASSPATH"
  CLASSPATH=${CLASSPATH}:"$PULSAR_HOME/$M/target/$JAR1";

  # additional resources
  if [ "$M" == "pulsar-app/pulsar-master" ]; then
    CLASSPATH=${CLASSPATH}:"$PULSAR_HOME/$M"/src/main/resources;
  fi
}

function __check_pid_before_start() {
    #ckeck if the process is not running
    mkdir -p "${PULSAR_PID_DIR}"
    if [ -f "$pid" ]; then
      if kill -0 "$(cat $pid)" > /dev/null 2>&1; then
        echo "$command" running as process "$(cat $pid)".  Stop it first.
        exit 1
      fi
    fi
}

function check_before_start() {
    #ckeck if the process is not running
    mkdir -p "$PULSAR_PID_DIR"
    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
    fi
}

function wait_until_done ()
{
    p=$1
    cnt=${PULSAR_SLAVE_TIMEOUT:-300}
    origcnt=$cnt
    while kill -0 $p > /dev/null 2>&1; do
      if [ $cnt -gt 1 ]; then
        cnt=`expr $cnt - 1`
        sleep 1
      else
        echo "Process did not complete after $origcnt seconds, killing."
        kill -9 $p
        exit 1
      fi
    done
    return 0
}
