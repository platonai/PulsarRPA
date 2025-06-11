#!/usr/bin/env bash

# 🔍 Find the first parent directory containing the VERSION file
APP_HOME=$(dirname $(readlink -f $0))
while [ ! -f "$APP_HOME/VERSION" ] && [ "$APP_HOME" != "/" ]; do
  APP_HOME=$(dirname "$APP_HOME")
done
cd "$APP_HOME"

echo "Update documentation ..."
echo "Changing version in documentation ..."

# Make sure we are not at master branch
if [[ "$(git rev-parse --abbrev-ref HEAD)" == "master" ]]; then
  echo "You are on the master branch. Please switch to a feature branch before running this script."
  exit 1
fi

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION/-SNAPSHOT/}
PREFIX=$(echo "$VERSION" | cut -d'.' -f1,2)

# Files containing the version number to upgrade
VERSION_AWARE_FILES=(
  "$APP_HOME/README.md"
  "$APP_HOME/README-CN.md"
)

# Replace version numbers in files
for F in "${VERSION_AWARE_FILES[@]}"; do
  if [ -e "$F" ]; then
    # Replace SNAPSHOT versions
    sed -i "s/$SNAPSHOT_VERSION/$VERSION/g" "$F"

    # Replace version numbers in the format "x.y.z" where x.y is the prefix and z is the minor version number
    sed -i "s/\b$PREFIX\.[0-9]\+\b/$VERSION/g" "$F"

    # Replace version numbers in paths like "download/v3.0.8/PulsarRPA.jar"
    sed -i "s|\(/v$PREFIX\.[0-9]\+/\)|/v$VERSION/|g" "$F"

    # Replace version numbers prefixed with v like "v3.0.8"
    sed -i "s/\bv$PREFIX\.[0-9]\+\b/v$VERSION/g" "$F"
  fi
done

# Commit changes and push to the repository
git add "${VERSION_AWARE_FILES[@]}"
git commit -m "Update documentation for version $VERSION"
git push
