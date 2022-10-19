#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

# Switching remote URLs from HTTPS to SSH
git remote set-url origin git@github.com:platonai/pulsar.git

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
LAST_COMMIT_ID=$(git log --format="%H" -n 1)
BRANCH=$(git branch --show-current)
TAG="v$VERSION"

function restore_working_branch() {
  echo "Ready to restore"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git restore .
  else
    echo "Bye."
    exit 0
  fi
}

function pull() {
  echo "Ready to pull"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git pull
  else
    echo "Bye."
    exit 0
  fi
}

function add_tag() {
  echo "Ready to add tag $TAG on $LAST_COMMIT_ID"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git tag "$TAG" "$LAST_COMMIT_ID"
    push_with_tags
  else
    echo "Do not add tag."
  fi
}

function push_with_tags() {
  echo "Ready to push with tags to $BRANCH"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git push --tags
  else
    echo "Do not push with tags"
  fi
}

function merge_to_main_branch() {
  echo
  git status

  echo "Ready to merge to main branch"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git checkout main

    # The main branch name is master
    exitCode=$?
    [ ! $exitCode -eq 0 ] && git checkout master

    git merge "$BRANCH"

    push_to_main_branch
  else
    echo "Do do merge to main branch."
  fi
}

function push_to_main_branch() {
  echo "Ready to push to main branch"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git push
  else
    echo "Bye."
    exit 0
  fi
}

function checkout_working_branch() {
  echo "Ready to checkout working branch $BRANCH"
  read -p "Are you sure to continue? [Y/n]" -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git checkout "$BRANCH"
  else
    echo "Remain on main branch"
  fi
}

restore_working_branch
pull
merge_to_main_branch
checkout_working_branch
add_tag
