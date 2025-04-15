#!/bin/bash

set -e  # 出错即停止
set -o pipefail

SOURCE_BRANCH=
# 输入分支名
read -p "Enter the branch name to sync: " SOURCE_BRANCH

if [ -z "$SOURCE_BRANCH" ]; then
  echo "❌ Branch name cannot be empty."
  exit 1
fi

# Check if the source branch exists
# Check if the branch exists locally
if git show-ref --verify --quiet "refs/heads/$SOURCE_BRANCH"; then
  echo "✅ Local branch '$SOURCE_BRANCH' exists."
elif git ls-remote --exit-code --heads origin "$SOURCE_BRANCH" > /dev/null; then
  echo "✅ Remote branch 'origin/$SOURCE_BRANCH' exists."
else
  echo "❌ Branch '$SOURCE_BRANCH' does not exist."
fi

# === 配置分支名 ===
MAIN_BRANCH="master"
BACKUP_BRANCH="backup-main-$(date +%Y%m%d-%H%M%S)"

exit 0

echo "🚀 [1/5] Fetching latest branches..."
git fetch origin

echo "📦 [2/5] Creating backup branch: $BACKUP_BRANCH"
git checkout $MAIN_BRANCH
git branch "$BACKUP_BRANCH"

echo "🔄 [3/5] Resetting $MAIN_BRANCH to match $SOURCE_BRANCH"
git reset --hard "origin/$SOURCE_BRANCH"

echo "🚀 [4/5] Pushing $MAIN_BRANCH to origin (force)"
git push -f origin "$MAIN_BRANCH"

echo "✅ [5/5] Done! '$MAIN_BRANCH' is now synced to 'origin/$SOURCE_BRANCH'."
echo "📂 Backup branch created: $BACKUP_BRANCH (local only)"
