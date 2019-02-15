#!/usr/bin/env bash

# change to your own privileged user name to the remote machine

USER=$USER
HOST=procanopus

this=`dirname "$0"`
bin=`cd "$this/..">/dev/null; pwd`

source "$bin"/include/pulsar-config.sh

OPTIONS=(--update -raz --progress)
PULSAR_VERSION=`cat $PULSAR_HOME/VERSION`

SOURCE="$PULSAR_HOME/pulsar-assembly/target/pulsar-$PULSAR_VERSION"
DESTINATION="$USER@$HOST:~/"

rsync ${OPTIONS[@]} $SOURCE $DESTINATION
