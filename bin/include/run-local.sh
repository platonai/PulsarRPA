#!/bin/bash

# local mode, add class paths
if [ "$PULSAR_RUNTIME_MODE" == "DEVELOPMENT" ]; then
  # development mode
  for f in "$PULSAR_HOME/$MODULE"/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done

  for f in "$PULSAR_HOME/$MODULE"/target/*.jar; do
    [[ ! sed-$f =~ -job.jar$ ]] && CLASSPATH=${CLASSPATH}:$f;
  done
elif [ "$PULSAR_RUNTIME_MODE" == "ASSEMBLY" ]; then
  # binary mode
  for f in "$PULSAR_HOME"/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done

  for f in "$PULSAR_HOME"/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done

  for f in "$PULSAR_HOME"/ext/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
  done

fi

PID="$PULSAR_PID_DIR/pulsar-$PULSAR_IDENT_STRING-$COMMAND.pid"

if [ "$COMMAND" = "master" ]; then
  PULSAR_LOG_PREFIX=pulsar-$PULSAR_IDENT_STRING-$COMMAND-$HOSTNAME
else
  PULSAR_LOG_PREFIX="pulsar-$PULSAR_IDENT_STRING-all-$HOSTNAME"
fi

PULSAR_LOGFILE="$PULSAR_LOG_PREFIX.log"
PULSAR_LOGOUT="$PULSAR_LOG_PREFIX.out"

# local mode
loglog="${PULSAR_LOG_DIR}/${PULSAR_LOGFILE}"
logout="${PULSAR_LOG_DIR}/${PULSAR_LOGOUT}"
logcmd="${PULSAR_TMP_DIR}/last-cmd-name"

PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dpulsar.home.dir=$PULSAR_HOME")
PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dpulsar.tmp.dir=$PULSAR_TMP_DIR")
PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dpulsar.id.str=$PULSAR_IDENT_STRING")
PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dpulsar.root.logger=${PULSAR_ROOT_LOGGER:-INFO,console}")
PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dlogging.dir=$PULSAR_LOG_DIR")
PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dlogging.file=$PULSAR_LOGFILE")

PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dglobal.executor.concurrency=${GLOBAL_EXECUTOR_CONCURRENCY:-1}")

if [[ $DRY_RUN == "1" ]]; then
    PULSAR_OPTS=("${PULSAR_OPTS[@]}" "-Dpulsar.dry.run=1")
fi

export CLASSPATH
if [[ $VERBOSE_LEVEL == "1" ]]; then
  echo $CLASSPATH
fi

JAVA="$JAVA_HOME/bin/java"

# fix for the external Xerces lib issue with SAXParserFactory
# PULSAR_OPTS=(-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl "${PULSAR_OPTS[@]}")
EXEC_CALL=("$JAVA" -Dproc_$COMMAND "-XX:OnOutOfMemoryError=\"kill -9 %p\"" $JAVA_HEAP_MAX "${PULSAR_OPTS[@]}")

MESSAGES=(
"============ Pulsar Runtime ================"
"\n`date`"
"\nCommand: $COMMAND"
"\nHostname: $(hostname)"
"\nVersion: " $("${PULSAR_HOME}"/bin/version)
"\nConfiguration directories: $PULSAR_CONF_DIR <= $PULSAR_PRIME_CONF_DIR, $PULSAR_EXTRA_CONF_DIR"
"\nWorking directory: `pwd`"
"\nPulsar home: " $PULSAR_HOME
"\nLog file: $loglog" "\n"
"\nCommand Line: " ${EXEC_CALL[@]} $CLASS $@ "\n"
)

if [[ $VERBOSE_LEVEL == "1" ]]; then
  echo -e "${MESSAGES[@]}"
fi
if [ ! -e $logout ]; then
  touch $logout
fi
echo -e "${MESSAGES[@]}" >> $logout
echo $COMMAND > $logcmd

# run it
if [[ "$RUN_AS_DAEMON" == "true" ]]; then
  exec "${EXEC_CALL[@]}" $CLASS "$@" >> $logout 2>&1 &
  echo $! > $PID
else
  exec "${EXEC_CALL[@]}" $CLASS "$@"
fi
