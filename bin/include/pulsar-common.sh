#!/usr/bin/env bash

function __dev_mode_enable_module() {
  M=$1
  shift

  # echo "Enable module $M"

  if [ "$M" == "pulsar-app" ]; then
    CLASSPATH=${CLASSPATH}:"$PULSAR_HOME/$M"/src/main/resources;
  fi

  # development mode
  for f in "$PULSAR_HOME/$M"/lib/*.jar; do
    if [[ -f "$f" ]]; then
      CLASSPATH=${CLASSPATH}:$f;
    fi
  done

  for f in "$PULSAR_HOME/$M"/target/*.jar; do
    [[ ! $f =~ (-tests|-sources|-job).jar$ ]] && CLASSPATH=${CLASSPATH}:$f;
  done
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
