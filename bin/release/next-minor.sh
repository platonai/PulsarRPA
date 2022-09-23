#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

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

# The following files contains the version number to upgrade
VERSION_AWARE_FILES=(
  "$APP_HOME/README.adoc"
  "$APP_HOME/README-CN.adoc"
)
# replace version numbers to be the next numbers in files
for F in "${VERSION_AWARE_FILES[@]}"; do
  sed -i "s/\b$PREFIX.[0-9]\{1,\}\b/$NEXT_VERSION/g" "$F";
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
