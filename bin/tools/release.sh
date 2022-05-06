#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

VERSION=$(head -n 1 "$APP_HOME/VERSION")
RESTORE_FILES="pulsar-ql-common/pom.xml pulsar-ql/pom.xml pulsar-plugins/pulsar-protocol-playwright/pom.xml pulsar-all/pom.xml pulsar-spring-support/pom.xml pulsar-spring-support/pulsar-beans/pom.xml pulsar-spring-support/pulsar-boot/pom.xml pulsar-rest/pom.xml pulsar-client/pom.xml pulsar-app/pom.xml pulsar-app/pulsar-app-resources/pom.xml pulsar-app/pulsar-app-resources/pulsar-app-common-resources/pom.xml pulsar-app/pulsar-sites-support/pom.xml pulsar-app/pulsar-sites-support/pulsar-site-amazon/pom.xml pulsar-plugins/pom.xml pulsar-app/pulsar-sites-support/pulsar-site-cn-gov/pom.xml pom.xml pulsar-app/pulsar-qa/pom.xml pulsar-common/pom.xml pulsar-app/pulsar-tests/pom.xml pulsar-persist/pom.xml pulsar-third/pom.xml pulsar-app/pulsar-master/pom.xml pulsar-app/pulsar-examples/pom.xml pulsar-third/pulsar-boilerpipe/pom.xml pulsar-skeleton/pom.xml pulsar-resources/pom.xml VERSION pulsar-plugins/pulsar-filter/pom.xml pulsar-plugins/pulsar-parse/pom.xml pulsar-plugins/pulsar-protocol/pom.xml pulsar-plugins/pulsar-scoring/pom.xml pulsar-dom/pom.xml pulsar-plugins/pulsar-index/pom.xml pulsar-plugins/pulsar-schedule/pom.xml pulsar-tools/pom.xml pulsar-tools/pulsar-browser/pom.xml"
LAST_COMMIT_ID=$(git log --format="%H" -n 1)
BRANCH=$(git branch --show-current)

read -p "Are you sure to release? " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
  echo ""
then
  git -c credential.helper= -c core.quotepath=false -c log.showSignature=false checkout "$BRANCH" -- "$RESTORE_FILES"
  git -c credential.helper= -c core.quotepath=false -c log.showSignature=false tag v"$VERSION" "$LAST_COMMIT_ID"
  git -c credential.helper= -c core.quotepath=false -c log.showSignature=false push --progress --porcelain origin refs/heads/"$BRANCH":"$BRANCH" --tags
fi
