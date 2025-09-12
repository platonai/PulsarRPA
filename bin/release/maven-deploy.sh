#bin

# Find the first parent directory that contains a VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit

printUsage() {
  echo "Usage: deploy [-clean|-test]"
}

if [[ $# -gt 0 ]]; then
  echo printUsage
  exit 0
fi

ENABLE_TEST=false
CLEAN=false

while [[ $# -gt 0 ]]; do
  case $1 in
    -clean)
      CLEAN=true
      shift # past argument
      ;;
    -test)
      ENABLE_TEST=true
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
  ./mvnw clean -Pall-modules
fi

if $ENABLE_TEST; then
  ./mvnw --batch-mode deploy -P deploy,release
else
  ./mvnw --batch-mode deploy -P deploy,release -DskipTests
fi

exitCode=$?
[ $exitCode -eq 0 ] && echo "Build successfully" || exit 1

# Build pulsar-app/pulsar-browser4 but do not deploy the artifacts
echo "Building pulsar-app/pulsar-browser4 ..."
cd "$APP_HOME/pulsar-app/pulsar-browser4" || exit
./mvnw install -DskipTests=true -Dmaven.javadoc.skip=true

exitCode=$?
[ $exitCode -eq 0 ] && echo "Build successfully" || exit 1

cd "$APP_HOME" || exit

echo "Artifacts are staged remotely, you should close and release the staging manually:"
echo "https://oss.sonatype.org/#stagingRepositories"
echo "Hit the following link to check if the artifacts are synchronized to the maven center: "
echo "https://repo1.maven.org/maven2/ai/platon/pulsar"
