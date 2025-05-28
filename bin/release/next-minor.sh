#bin

# Find the first parent directory that contains a pom.xml file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$APP_HOME" != "/" ]]; do
  if [[ -f "$APP_HOME/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$APP_HOME")
done

cd "$APP_HOME" || exit

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
PREFIX=$(echo "$VERSION" | cut -d'.' -f1,2)
MINOR_VERSION=$(echo "$VERSION" | cut -d'.' -f3)
MINOR_VERSION=$(("$MINOR_VERSION" + 1))

NEXT_VERSION="$PREFIX.$MINOR_VERSION"
NEXT_SNAPSHOT_VERSION="$NEXT_VERSION-SNAPSHOT"

echo "New version: $NEXT_SNAPSHOT_VERSION"
# VERSION file
echo "$NEXT_SNAPSHOT_VERSION" > "$APP_HOME"/VERSION
# $APP_HOME/pom.xml
sed -i -e "s/<tag>v$VERSION<\/tag>/<tag>v$NEXT_VERSION<\/tag>/g" "$APP_HOME/pom.xml";
# pom.xml files
find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$NEXT_SNAPSHOT_VERSION/" {} \;

# Files containing the version number to upgrade
VERSION_AWARE_FILES=(
  "$APP_HOME/README.md"
  "$APP_HOME/README-CN.md"
)

# Replace version numbers in files
for F in "${VERSION_AWARE_FILES[@]}"; do
  if [ -e "$F" ]; then
    # Replace SNAPSHOT versions
    sed -i "s/$SNAPSHOT_VERSION/$NEXT_SNAPSHOT_VERSION/g" "$F"

    # Replace version numbers in the format "x.y.z" where x.y is the prefix and z is the minor version number
    sed -i "s/\b$PREFIX\.[0-9]\+\b/$NEXT_VERSION/g" "$F"

    # Replace version numbers in paths like "download/v3.0.8/PulsarRPA.jar"
    sed -i "s|\(/v$PREFIX\.[0-9]\+/\)|/v$NEXT_VERSION/|g" "$F"

    # Replace version numbers prefixed with v like "v3.0.8"
    sed -i "s/\bv$PREFIX\.[0-9]\+\b/v$NEXT_VERSION/g" "$F"
  fi
done

COMMENT=${NEXT_SNAPSHOT_VERSION//"-SNAPSHOT"/""}

echo "Ready to commit with comment: <$COMMENT>"
read -p "Are you sure to continue? [Y/n]" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git add .
  git commit -m "$COMMENT"
  git push
fi
