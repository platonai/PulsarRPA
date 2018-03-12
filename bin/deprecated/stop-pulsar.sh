#!/usr/bin/env bash
#
#/**
# * Copyright 2007 The Apache Software Foundation
# *
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

# Modelled after $HADOOP_HOME/bin/stop-pulsar.sh.

# Stop hadoop pulsar daemons.  Run this on master node.

function printUsage() {
  echo "Usage: stop-pulsar [options...]"

  echo
  echo "Options: "
  echo "  -f, --force      Use kill instead of native stop command"
  echo "  -v, --verbose    Talk more"
  echo "  -h, --help       The help text"
  exit 1
}

while [ $# -gt 1 ]
do
case $1 in
    -f|--force)
        FORCE=true
        shift
        ;;
    -v|--verbose)
        export VERBOSE_LEVEL=1
        shift
        ;;
    -h|--help)
        SHOW_HELP=true
        shift
        ;;
    -*)
        echo "Unrecognized option : $1"
        echo "Try 'pulsar --help' for more information."
        exit 0
        ;;
    *)
        # command begins
        break
        ;;
esac
done

if [[ $SHOW_HELP ]]; then
  printUsage;
  exit 0
fi

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`

. "$bin"/include/pulsar-config.sh
. "$bin"/include/pullsar-common.sh

if [ "$PULSAR" = "" ]; then
  export PULSAR="$bin/pulsar"
fi

# variables needed for stop command
if [ "$PULSAR_LOG_DIR" = "" ]; then
  export PULSAR_LOG_DIR="$PULSAR_HOME/logs"
fi
mkdir -p "$PULSAR_LOG_DIR"

if [ "$PULSAR_IDENT_STRING" = "" ]; then
  export PULSAR_IDENT_STRING="$USER"
fi

export PULSAR_LOG_PREFIX=pulsar-$PULSAR_IDENT_STRING-master-$HOSTNAME
export PULSAR_LOGFILE=$PULSAR_LOG_PREFIX.log
logout=$PULSAR_LOG_DIR/$PULSAR_LOG_PREFIX.out  
loglog="${PULSAR_LOG_DIR}/${PULSAR_LOGFILE}"
pid=${PULSAR_PID_DIR:-/tmp}/pulsar-$PULSAR_IDENT_STRING-master.pid

echo -n stopping pulsar
echo
echo "`date` Stopping pulsar (via master)" >> $loglog

# the safe way
if [[ $FORCE ]]; then
  # simpler but not safe
  "$bin"/pulsar-daemon.sh --config "${PULSAR_EXTRA_CONF_DIR}" stop master $@
else
  nohup nice -n ${PULSAR_NICENESS:-0} \
    $PULSAR --config "${PULSAR_EXTRA_CONF_DIR}" master stop "$@" > "$logout" 2>&1 < /dev/null &
  waitForProcessEnd `cat $pid` 'stop-master-command'
fi

rm -f $pid
