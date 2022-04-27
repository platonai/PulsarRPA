#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=$(sed 's/\(.*\)-.*/\1/' <<< $SNAPSHOT_VERSION)
PREFIX=$(echo "$VERSION" | cut -d'.' -f1,2)
MINOR_VERSION=$(echo "$VERSION" | cut -d'.' -f3)
MINOR_VERSION=$(("$MINOR_VERSION" + 1))

NEXT_VERSION="$PREFIX.$MINOR_VERSION-SNAPSHOT"

echo "Next version: $NEXT_VERSION"
echo "$NEXT_VERSION" > "$APP_HOME"/VERSION
find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$NEXT_VERSION/" {} \;
