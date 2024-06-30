#!/usr/bin/env bash

bin=$(dirname "$0")
bin=$(cd "$bin">/dev/null || exit; pwd)

 . "$bin"/include/config.sh

VERSION="v$(cat "$APP_HOME"/VERSION)"

if [ $# -gt 0 ] && [ "$1" == "-v" ]; then
  # dynamically pull more interesting stuff from latest git commit
  HASH=$(git show-ref --head --hash=7 head)            # first 7 letters of hash should be enough; that's what GitHub uses
  BRANCH=$(git rev-parse --abbrev-ref HEAD)
  DATE=$(git log -1 --pretty=%ad --date=short)
  # Return the version string used to describe this version of Metabase.
  echo "$VERSION $HASH $BRANCH $DATE"
else
  echo "$VERSION"
fi
