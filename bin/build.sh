#!/usr/bin/env bash

bin=$(dirname "$0")
bin=$(cd "$bin">/dev/null || exit; pwd)

"$bin"/tools/install-depends.sh

mvn clean && mvn -Pall-modules
