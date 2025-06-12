#!/usr/bin/env bash

set -euo pipefail

# === Function: run git command with error handling ===
run_git() {
  echo "▶️ git $*" >&2
  if ! git "$@"; then
    echo "❌ Git command failed: git $*" >&2
    exit 1
  fi
}

# === Input ===
read -rp "Enter the branch name to sync: " SOURCE_BRANCH
if [[ -z "$SOURCE_BRANCH" ]]; then
  echo "❌ Branch name cannot be empty." >&2
  exit 1
fi

# === Check if remote branch exists ===
if git ls-remote --exit-code --heads origin "$SOURCE_BRANCH" >/dev/null; then
  echo "✅ Remote branch 'origin/$SOURCE_BRANCH' exists."
else
  echo "❌ Remote branch 'origin/$SOURCE_BRANCH' does not exist." >&2
  exit 1
fi

# === Define constants ===
MAIN_BRANCH="master"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
BACKUP_BRANCH="${MAIN_BRANCH}-backup-${TIMESTAMP}"

# === Step 1: Fetch ===
echo "🚀 [1/5] Fetching origin..."
run_git fetch origin

# === Step 2: Checkout and create backup ===
echo "📦 [2/5] Checking out '$MAIN_BRANCH' and creating backup: '$BACKUP_BRANCH'"
run_git checkout "$MAIN_BRANCH"
run_git branch "$BACKUP_BRANCH"

# === Step 3: Reset master ===
echo "🔄 [3/5] Resetting '$MAIN_BRANCH' to match 'origin/$SOURCE_BRANCH'"
run_git fetch origin "$SOURCE_BRANCH"
run_git reset --hard "origin/$SOURCE_BRANCH"

# === Step 4: Force push ===
echo "☁️ [4/5] Force pushing '$MAIN_BRANCH' to origin..."
run_git push -f origin "$MAIN_BRANCH"

# === Done ===
echo
echo "✅ [5/5] Done!"
echo "🌿 '$MAIN_BRANCH' is now synced to 'origin/$SOURCE_BRANCH'"
echo "📂 Local backup created: '$BACKUP_BRANCH'"
