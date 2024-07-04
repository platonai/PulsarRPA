#bin

# Find the first parent directory that contains a pom.xml file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$APP_HOME" != "/" ]]; do
  if [[ -f "$APP_HOME/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$APP_HOME")
done

printUsage() {
  echo "Usage: deploy [-clean|-test]"
}

if [[ $# -gt 0 ]]; then
  echo printUsage
  exit 0
fi

TEST=false
CLEAN=false

while [[ $# -gt 0 ]]; do
  case $1 in
    -clean)
      CLEAN=true
      shift # past argument
      ;;
    -test)
      TEST=true
      shift # past argument
      ;;
    -h|-help|--help)
      printUsage
      exit 1
      ;;
    -*|--*)
      printUsage
      exit 1
      ;;
    *)
      shift # past argument
      ;;
  esac
done

echo "Deploy the project ..."
echo "Changing version ..."

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
echo "$VERSION" > "$APP_HOME"/VERSION

find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$VERSION/" {} \;

if $CLEAN; then
  mvn clean
fi

if $TEST; then
  mvn deploy -Pplaton-release -Pplaton-deploy
else
  mvn deploy -Pplaton-release -Pplaton-deploy -DskipTests=true
fi

exitCode=$?
[ $exitCode -eq 0 ] && echo "Build successfully" || exit 1

echo "Artifacts are staged remotely, you should close and release the staging manually:"
echo "https://oss.sonatype.org/#stagingRepositories"
echo "Hit the following link to check if the artifacts are synchronized to the maven center: "
echo "https://repo1.maven.org/maven2/ai/platon/pulsar"
