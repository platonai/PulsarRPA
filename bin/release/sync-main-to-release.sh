#!/bin/bash

set -e  # 出错即停止
set -o pipefail

# === 配置分支名 ===
MAIN_BRANCH="main"
SOURCE_BRANCH="release"
BACKUP_BRANCH="backup-main-$(date +%Y%m%d-%H%M%S)"

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
