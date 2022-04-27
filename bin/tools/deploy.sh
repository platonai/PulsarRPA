#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

echo "Deploy the project ..."
echo "Changing version ..."

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=$(sed 's/\(.*\)-.*/\1/' <<< $SNAPSHOT_VERSION)
echo "$VERSION" > "$APP_HOME"/VERSION
find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$VERSION/" {} \;

mvn clean
mvn deploy -Pall-modules -Pplaton-release -Pplaton-deploy
