#!/bin/bash

this="${BASH_SOURCE-$0}"
# Convert relative path to absolute path
bin=$(dirname "$this")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
script=$(basename "$this")
this="$bin/$script"

 . "$bin"/include/config.sh

find "$APP_HOME"/bin -type f -print0 | xargs -0 dos2unix
dos2unix "$APP_HOME"/VERSION

