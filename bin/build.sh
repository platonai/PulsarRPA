#!/usr/bin/env bash

bin=$(dirname "$0")
bin=$(cd "$bin">/dev/null || exit; pwd)

mvn clean && mvn -Pall-modules
