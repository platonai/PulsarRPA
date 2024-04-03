#!/usr/bin/env bash

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
if [ "$APP_HOME" = "" ]; then
  # Convert relative path to absolute path
  bin=$(dirname "$this")/..
  script=$(basename "$this")
  this="$bin/$script"
  bin=$(cd "$bin">/dev/null || exit; pwd)

  APP_HOME=$(dirname "$bin")
  APP_HOME=$(cd "$APP_HOME">/dev/null || exit; pwd)
  export APP_HOME=$APP_HOME
fi

if [ "$APP_RUNTIME_MODE" = ""  ]; then
  if [ -f "$APP_HOME/pom.xml" ]; then
    export APP_RUNTIME_MODE="DEVELOPMENT"
  else
    export APP_RUNTIME_MODE="ASSEMBLY"
  fi
fi

# The log dir
if [ "$APP_LOG_DIR" = "" ]; then
  export APP_LOG_DIR="$APP_HOME/logs"
fi
mkdir -p "$APP_LOG_DIR"

# The ident string
if [ "$APP_IDENT_STRING" = "" ]; then
  export APP_IDENT_STRING="$USER"
fi

# The program tmp dir
if [ "$APP_TMP_DIR" = "" ]; then
  export APP_TMP_DIR="/tmp/pulsar-$APP_IDENT_STRING"
fi
mkdir -p "$APP_TMP_DIR"

# The directory to keep process PIDs
if [ "$APP_PID_DIR" = "" ]; then
  export APP_PID_DIR="$APP_TMP_DIR"
fi

if [[ "$APP_NICENESS" = "" ]]; then
    export APP_NICENESS=0
fi

# Source the pulsar-env.sh.  Will have JAVA_HOME defined
if [ -z "$APP_ENV_INIT" ]; then
  [ -f "${APP_CONF_DIR}/pulsar-env.sh" ] && . "${APP_CONF_DIR}/pulsar-env.sh"
  [ -f "$HOME/.pulsar/pulsar-env.sh" ] && . $HOME/.pulsar/pulsar-env.sh

  export APP_ENV_INIT="true"
fi

if [[ -z "$JAVA_HOME" ]]; then
  # Now having JAVA_HOME defined is required
  JAVAC=$(command -v javac)
  if [[ -e $JAVAC ]]; then
    JAVA_HOME=$(readlink -f "$JAVAC" | sed "s:bin/javac::")
    export JAVA_HOME
  fi
fi

# Now having JAVA_HOME defined is required
if [[ -z "$JAVA_HOME" ]]; then
    cat 1>&2 <<EOF
+======================================================================+
|      Error: JAVA_HOME is not set and javac could not be found        |
+======================================================================+
EOF
    exit 1
fi
