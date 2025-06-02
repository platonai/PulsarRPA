#!/usr/bin/env bash

# 🔍 Find the first parent directory containing the VERSION file
APP_HOME=$(dirname $(readlink -f $0))
while [ ! -f "$APP_HOME/VERSION" ] && [ "$APP_HOME" != "/" ]; do
  APP_HOME=$(dirname "$APP_HOME")
done
cd "$APP_HOME"

echo "Deploy the project ..."
echo "Changing version ..."

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION/-SNAPSHOT/}
echo "$VERSION" > "$APP_HOME/VERSION"

# Replace SNAPSHOT version with the release version
for FILE_PATTERN in 'llm-config.md' 'README-CN.md' 'pom.xml'; do
  find "$APP_HOME" -maxdepth 3 -name "$FILE_PATTERN" -type f | while read FILE; do
    sed -i "s/$SNAPSHOT_VERSION/$VERSION/g" "$FILE"
  done
done