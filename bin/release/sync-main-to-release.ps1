#Requires -Version 5.0
$ErrorActionPreference = "Stop"

# === 输入分支名 ===
$SOURCE_BRANCH = Read-Host "Enter the branch name to sync"

if ([string]::IsNullOrWhiteSpace($SOURCE_BRANCH)) {
    Write-Host "❌ Branch name cannot be empty." -ForegroundColor Red
    exit 1
}

# === 检查分支是否存在 ===
$branchExistsLocal = git show-ref --verify --quiet "refs/heads/$SOURCE_BRANCH"
$branchExistsRemote = git ls-remote --exit-code --heads origin "$SOURCE_BRANCH" *> $null

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Remote branch 'origin/$SOURCE_BRANCH' exists." -ForegroundColor Green
} elseif (git show-ref --verify --quiet "refs/heads/$SOURCE_BRANCH") {
    Write-Host "✅ Local branch '$SOURCE_BRANCH' exists." -ForegroundColor Green
} else {
    Write-Host "❌ Branch '$SOURCE_BRANCH' does not exist." -ForegroundColor Red
    exit 1
}

# === 配置主分支与备份分支名 ===
$MAIN_BRANCH = "master"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BACKUP_BRANCH = "backup-main-$timestamp"

Write-Host "🚀 [1/5] Fetching latest branches..."
git fetch origin

Write-Host "📦 [2/5] Creating backup branch: $BACKUP_BRANCH"
git checkout $MAIN_BRANCH
git branch $BACKUP_BRANCH

Write-Host "🔄 [3/5] Resetting $MAIN_BRANCH to match $SOURCE_BRANCH"
git reset --hard "origin/$SOURCE_BRANCH"

Write-Host "🚀 [4/5] Pushing $MAIN_BRANCH to origin (force)"
git push -f origin $MAIN_BRANCH

Write-Host "✅ [5/5] Done! '$MAIN_BRANCH' is now synced to 'origin/$SOURCE_BRANCH'." -ForegroundColor Green
Write-Host "📂 Backup branch created: $BACKUP_BRANCH (local only)"
