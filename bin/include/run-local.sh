#!/bin/bash

# local mode, add class paths
if [[ $APP_RUNTIME_MODE == "DEVELOPMENT" ]]; then
  # development mode
  __dev_mode_enable_module "$MODULE" "$JAR1"
elif [[ $APP_RUNTIME_MODE == "ASSEMBLY" ]]; then
  for f in "$APP_HOME"/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done

  for f in "$APP_HOME"/ext/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done

  for f in "$APP_HOME"/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done
fi

PID="$APP_PID_DIR/pulsar-$APP_IDENT_STRING-$COMMAND.pid"

if [[ "$CLASS" == *"JobRunner"* ]]; then
  APP_LOG_PREFIX="pulsar-$APP_IDENT_STRING-$COMMAND-$HOSTNAME"
else
  APP_LOG_PREFIX=pulsar-$APP_IDENT_STRING-all-$HOSTNAME
fi

APP_LOGFILE="$APP_LOG_PREFIX.log"
APP_LOGOUT="$APP_LOG_PREFIX.out"

# local mode
loglog="${APP_LOG_DIR}/${APP_LOGFILE}"
logout="${APP_LOG_DIR}/${APP_LOGOUT}"
logcmd="${APP_TMP_DIR}/last-cmd-name"

APP_OPTS=("${APP_OPTS[@]}" "-Duser.dir=$APP_HOME")
APP_OPTS=("${APP_OPTS[@]}" "-Dapp.name=$APP_NAME")
APP_OPTS=("${APP_OPTS[@]}" "-Dapp.id.str=$APP_IDENT_STRING")
APP_OPTS=("${APP_OPTS[@]}" "-Dapp.tmp.dir=$APP_TMP_DIR")
APP_OPTS=("${APP_OPTS[@]}" "-Dlogging.dir=$APP_LOG_DIR")
APP_OPTS=("${APP_OPTS[@]}" "-Dlogging.file=$APP_LOGFILE")

export CLASSPATH
if [[ $VERBOSE_LEVEL == "1" ]]; then
  echo "$CLASSPATH"
fi

JAVA="$JAVA_HOME/bin/java"

EXEC_CALL=("$JAVA" -Dproc_"$COMMAND" "${APP_OPTS[@]}")

MESSAGES=(
"============ Pulsar Runtime ================"
"\n$(date)"
"\nCommand: $COMMAND"
"\nHostname: $(hostname)"
"\nVersion: $VERSION"
"\nWorking directory: $(pwd)"
"\nApp home: " "$APP_HOME"
"\nLog file: $loglog" "\n"
"\nCommand Line: " "${EXEC_CALL[@]}" "$CLASS" "$@" "\n"
)

if [[ $VERBOSE_LEVEL == "1" ]]; then
  echo -e "${MESSAGES[@]}"
fi
if [ ! -e "$logout" ]; then
  touch "$logout"
fi
echo -e "${MESSAGES[@]}" >> "$logout"
echo "$COMMAND" > "$logcmd"

# run it
if [[ "$RUN_AS_DAEMON" == "true" ]]; then
  exec "${EXEC_CALL[@]}" "$CLASS" "$@" >> "$logout" 2>&1 &
  echo $! > "$PID"
else
  exec "${EXEC_CALL[@]}" "$CLASS" "$@"
fi
