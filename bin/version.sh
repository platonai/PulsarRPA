#!/usr/bin/env bash

# Find the first parent directory that contains a pom.xml file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$APP_HOME" != "/" ]]; do
  if [[ -f "$APP_HOME/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$APP_HOME")
done

cd "$APP_HOME" || exit

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
