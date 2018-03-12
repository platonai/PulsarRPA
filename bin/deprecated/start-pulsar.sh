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

# Start daemons.
# Run this on master node.
function printUsage() {
  echo "Usage: stop-pulsar.sh [options...]"

  echo
  echo "Options: "
  echo "  -c, --config     Config dir"
  echo "      --rotate-log Rotate logs"
  echo "  -v, --verbose    Talk more"
  echo "  -h, --help       The help text"
  exit 1
}

while [ $# -gt 1 ]
do
case $1 in
    -c|--config)
        shift
        PULSAR_EXTRA_CONF_DIR=$1
        shift
        ;;
    --rotate-log)
        export ROTATE_LOG=true
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

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`

 . "$bin"/include/pulsar-config.sh

# start hbase daemons
errCode=$?
if [ $errCode -ne 0 ]
then
  exit $errCode
fi

if [ "$1" = "autorestart" ]; then
  commandToRun="autorestart"
else
  commandToRun="start"
fi

OPTIONS=(--config "$PULSAR_EXTRA_CONF_DIR")
if [[ ${ROTATE_LOG} ]]; then
  OPTIONS=(${OPTIONS[@]} "--rotate-log")
fi

"$bin"/pulsar-daemon.sh ${OPTIONS[@]} $commandToRun master $@
