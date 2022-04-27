#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

mvn clean

echo "Deploy the project ..."
echo "Changing version ..."

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=$(sed 's/\(.*\)-.*/\1/' <<< $SNAPSHOT_VERSION)
echo "$VERSION" > "$APP_HOME"/VERSION
for f in */pom.xml; do
  sed -i "s/$SNAPSHOT_VERSION/$VERSION/" "$f"
done

mvn deploy -Pall-modules -Pplaton-release -Pplaton-deploy
