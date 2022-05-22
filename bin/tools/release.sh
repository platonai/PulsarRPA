#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
LAST_COMMIT_ID=$(git log --format="%H" -n 1)
BRANCH=$(git branch --show-current)
TAG="v$VERSION"

echo "Ready to checkout branch $BRANCH"
read -p "Are you sure to continue?Yy" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git checkout "$BRANCH"
  echo ""
else
  echo "Bye."
  exit 0
fi

echo "Ready to add tag $TAG on $LAST_COMMIT_ID"
read -p "Are you sure to continue?Yy" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git tag "$TAG" "$LAST_COMMIT_ID"
else
  echo "Bye."
  exit 0
fi

echo "Ready to push with tags to $BRANCH"
read -p "Are you sure to continue?Yy" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git push --tags
else
  echo "Bye."
  exit 0
fi

echo "Ready to merge to main branch"
read -p "Are you sure to continue?Yy" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git checkout main
  git merge "$BRANCH"
else
  echo "Bye."
  exit 0
fi

echo "Ready to push to main branch"
read -p "Are you sure to continue?Yy" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git push
else
  echo "Bye."
  exit 0
fi

echo "Ready to checkout $BRANCH"
read -p "Are you sure to continue?Yy" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git checkout "$BRANCH"
else
  echo "Bye."
  exit 0
fi
