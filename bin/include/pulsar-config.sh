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

# included in all the hbase scripts with source command
# should not be executable directly
# also should not be passed any arguments, since we need original $*
# Modelled after $HADOOP_HOME/bin/hadoop-env.sh.

# resolve links - "${BASH_SOURCE-$0}" may be a softlink
this="${BASH_SOURCE-$0}"
while [ -h "$this" ]; do
  ls=$(ls -ld "$this")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '.*/.*' > /dev/null; then
    this="$link"
  else
    this=$(dirname "$this")/"$link"
  fi
done

# The root dir of this program
if [ "$PULSAR_HOME" = "" ]; then
  # Convert relative path to absolute path
  bin=$(dirname "$this")/..
  script=$(basename "$this")
  this="$bin/$script"
  bin=$(cd "$bin">/dev/null || exit; pwd)

  PULSAR_HOME=$(dirname "$bin")
  PULSAR_HOME=$(cd "$PULSAR_HOME">/dev/null || exit; pwd)
  export PULSAR_HOME=$PULSAR_HOME
fi

# Detect the runtime mode
for f in "${PULSAR_HOME}"/pulsar-enterprise-*-job.jar; do
  if [ -f "$f" ]; then
    export PULSAR_RUNTIME_MODE="PACKED"
  fi
done

if [ "$PULSAR_RUNTIME_MODE" = ""  ]; then
  if [ -f "$PULSAR_HOME/pom.xml" ]; then
    export PULSAR_RUNTIME_MODE="DEVELOPMENT"
  else
    export PULSAR_RUNTIME_MODE="ASSEMBLY"
  fi
fi

# The log dir
if [ "$PULSAR_LOG_DIR" = "" ]; then
  export PULSAR_LOG_DIR="$PULSAR_HOME/logs"
fi
mkdir -p "$PULSAR_LOG_DIR"

# The ident string
if [ "$PULSAR_IDENT_STRING" = "" ]; then
  export PULSAR_IDENT_STRING="$USER"
fi

# The program tmp dir
if [ "$PULSAR_TMP_DIR" = "" ]; then
  export PULSAR_TMP_DIR="/tmp/pulsar-$PULSAR_IDENT_STRING"
fi
mkdir -p "$PULSAR_TMP_DIR"

# The directory to keep process PIDs
if [ "$PULSAR_PID_DIR" = "" ]; then
  export PULSAR_PID_DIR="$PULSAR_TMP_DIR"
fi

# The key and password to connect the H2 TCP server instance
if [ "$PULSAR_DB_KEY" = "" ]; then
  export PULSAR_DB_KEY="pulsar"
fi
if [ "$PULSAR_DB_PASSWORD" = "" ]; then
  export PULSAR_DB_PASSWORD="pulsar"
fi

if [[ "$PULSAR_NICENESS" = "" ]]; then
    export PULSAR_NICENESS=0
fi

# Source the hbase-env.sh.  Will have JAVA_HOME defined
if [ -z "$PULSAR_ENV_INIT" ]; then
  [ -f "${PULSAR_CONF_DIR}/pulsar-env.sh" ] && . "${PULSAR_CONF_DIR}/pulsar-env.sh"
  [ -f "$HOME/.pulsar/pulsar-env.sh" ] && . $HOME/.pulsar/pulsar-env.sh

  export PULSAR_ENV_INIT="true"
fi

# Newer versions of glibc use an arena memory allocator that causes virtual
# memory usage to explode. Tune the variable down to prevent vmem explosion.
export MALLOC_ARENA_MAX=${MALLOC_ARENA_MAX:-4}

# Now having JAVA_HOME defined is required 
if [[ -z "$JAVA_HOME" ]]; then
    cat 1>&2 <<EOF
+======================================================================+
|      Error: JAVA_HOME is not set and Java could not be found         |
+======================================================================+
EOF
    exit 1
fi

if [[ "$JAVA_HOME" != "/usr/lib/jvm/java-8-sun" ]]; then
  echo "Since the latest gora still uses hadoop-2.5.2 which is not compitable with java 9 or later, we have to use java 8"
  exit 1
fi
