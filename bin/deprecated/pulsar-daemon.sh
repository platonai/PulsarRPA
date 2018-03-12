#!/usr/bin/env bash

usage="Usage: pulsar-daemon.sh [options]\
 (start|stop|restart|autorestart) <pulsar-command> \
 <args...>"

# Start daemons.
# Run this on master node.
function printUsage() {
  echo $usage

  echo
  echo "Options: "
  echo "  -c, --config     Config dir"
  echo "      --rotate-log Rotate logs"
  echo "  -v, --verbose    Talk more"
  echo "  -h, --help       The help text"
  exit 1
}

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

while [ $# -gt 1 ]
do
case $1 in
    -c|--config)
        shift
        export PULSAR_EXTRA_CONF_DIR="$1"
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
        # commands begins
        break
        ;;
esac
done

# get arguments
startStop=$1
shift

command=$1
shift

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`

source "$bin"/include/pulsar-config.sh
source "$bin"/include/pullsar-common.sh

if [ "$PULSAR" = "" ]; then
  export PULSAR="$bin/pulsar"
fi

# get log directory
if [ "$PULSAR_LOG_DIR" = "" ]; then
  export PULSAR_LOG_DIR="$PULSAR_HOME/logs"
fi
mkdir -p "$PULSAR_LOG_DIR"

if [ "$PULSAR_PID_DIR" = "" ]; then
  export PULSAR_PID_DIR=/tmp
fi

if [ "$PULSAR_IDENT_STRING" = "" ]; then
  export PULSAR_IDENT_STRING="$USER"
fi

# Some variables
# Work out java location so can print version into log.
if [ "$JAVA_HOME" != "" ]; then
  #echo "run java in $JAVA_HOME"
  JAVA_HOME=$JAVA_HOME
fi
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

JAVA=$JAVA_HOME/bin/java
export PULSAR_LOG_PREFIX=pulsar-$PULSAR_IDENT_STRING-$command-$HOSTNAME
export PULSAR_LOGFILE=$PULSAR_LOG_PREFIX.log

if [ -z "${PULSAR_ROOT_LOGGER}" ]; then
  export PULSAR_ROOT_LOGGER=${PULSAR_ROOT_LOGGER:-"INFO,RFA"}
fi

if [ -z "${PULSAR_SECURITY_LOGGER}" ]; then 
  export PULSAR_SECURITY_LOGGER=${PULSAR_SECURITY_LOGGER:-"INFO,RFAS"}
fi

logout=$PULSAR_LOG_DIR/$PULSAR_LOG_PREFIX.out
loggc=$PULSAR_LOG_DIR/$PULSAR_LOG_PREFIX.gc
loglog="${PULSAR_LOG_DIR}/${PULSAR_LOGFILE}"
pid=$PULSAR_PID_DIR/pulsar-$PULSAR_IDENT_STRING-$command.pid
export PULSAR_START_FILE=$PULSAR_PID_DIR/pulsar-$PULSAR_IDENT_STRING-$command.autorestart

if [ -n "$SERVER_GC_OPTS" ]; then
  export SERVER_GC_OPTS=${SERVER_GC_OPTS/"-Xloggc:<FILE-PATH>"/"-Xloggc:${loggc}"}
fi
if [ -n "$CLIENT_GC_OPTS" ]; then
  export CLIENT_GC_OPTS=${CLIENT_GC_OPTS/"-Xloggc:<FILE-PATH>"/"-Xloggc:${loggc}"}
fi

# Set default scheduling priority
if [ "$PULSAR_NICENESS" = "" ]; then
  export PULSAR_NICENESS=0
fi

thiscmd="$bin/$(basename ${BASH_SOURCE-$0})"
args=$@

case $startStop in

(start)
    check_before_start
    if [[ $ROTATE_LOG = "true" ]]; then
      pulsar_rotate_log $logout
      pulsar_rotate_log $loggc
    fi
    echo starting $command, logging to $logout
    nohup $thiscmd --config "${PULSAR_EXTRA_CONF_DIR}" internal_start $command $args < /dev/null > ${logout} 2>&1  &
    echo $! > $pid
    sleep 1; head "${logout}"
  ;;

(autorestart)
    check_before_start
    if [[ $ROTATE_LOG = "true" ]]; then
      pulsar_rotate_log $logout
      pulsar_rotate_log $loggc
    fi
    nohup $thiscmd --config "${PULSAR_EXTRA_CONF_DIR}" internal_autorestart $command $args < /dev/null > ${logout} 2>&1  &
  ;;

(internal_start)
    # Add to the command log file vital stats on our environment.
    echo "`date` Starting $command on `hostname`" >> $loglog
    echo "`ulimit -a`" >> $loglog 2>&1
    nice -n $PULSAR_NICENESS \
      "$PULSAR" --config "${PULSAR_EXTRA_CONF_DIR}" --enterprise $command "$@" start >> "$logout" 2>&1 &
    echo $! > $pid
    wait
  ;;

(internal_autorestart)
    touch "$PULSAR_START_FILE"
    #keep starting the command until asked to stop. Reloop on software crash
    while true
      do
        lastLaunchDate=`date +%s`
        $thiscmd --config "${PULSAR_EXTRA_CONF_DIR}" internal_start $command $args

        #if the file does not exist it means that it was not stopped properly by the stop command
        if [ ! -f "$PULSAR_START_FILE" ]; then
          exit 1
        fi

        #if it was launched less than 5 minutes ago, then wait for 5 minutes before starting it again.
        curDate=`date +%s`
        limitDate=`expr $lastLaunchDate + 300`
        if [ $limitDate -gt $curDate ]; then
          sleep 300
        fi
      done
    ;;

(stop)
    rm -f "$PULSAR_START_FILE"
    if [ -f $pid ]; then
      pidToKill=`cat $pid`
      # kill -0 == see if the PID exists
      if kill -0 $pidToKill > /dev/null 2>&1; then
        echo -n stopping $command
        echo "`date` Terminating $command" >> $loglog
        kill $pidToKill > /dev/null 2>&1
        waitForProcessEnd $pidToKill $command
      else
        retval=$?
        echo no $command to stop because kill -0 of pid $pidToKill failed with status $retval
      fi
    else
      echo no $command to stop because no pid file $pid
    fi
    rm -f $pid
  ;;

(restart)
    # stop the command
    $thiscmd --config "${PULSAR_EXTRA_CONF_DIR}" stop $command $args &
    wait_until_done $!
    # wait a user-specified sleep period
    sp=${PULSAR_RESTART_SLEEP:-3}
    if [ $sp -gt 0 ]; then
      sleep $sp
    fi
    # start the command
    $thiscmd --config "${PULSAR_EXTRA_CONF_DIR}" start $command $args &
    wait_until_done $!
  ;;

(*)
  echo $usage
  exit 1
  ;;
esac
