#!/usr/bin/env bash

bin=$(dirname "$0")
bin=$(cd "$bin">/dev/null || exit; pwd)

cd pulsar-third/gora-shaded-mongodb
mvn

cd - || exit
mvn -DskipTests=true

bin/tools/install-depends.sh

bin/pulsar
