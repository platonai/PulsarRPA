#!/usr/bin/env bash

bin=$(dirname "$0")
bin=$(cd "$bin">/dev/null || exit; pwd)

mvn clean && mvn -DskipTests=true

"$bin"/tools/install-depends.sh

"$bin"/pulsar
