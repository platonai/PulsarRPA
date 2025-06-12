#Requires -Version 5.0
$ErrorActionPreference = "Stop"

function Run-Git {
    param (
        [Parameter(Mandatory = $true)][string]$Command,
        [string]$ErrorMessage = "❌ Git command failed."
    )

    Write-Host "▶️ git $Command" -ForegroundColor Gray
    git $Command

    if ($LASTEXITCODE -ne 0) {
        Write-Host $ErrorMessage -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

# === Step 0: 输入源分支名 ===
$SOURCE_BRANCH = Read-Host "Enter the branch name to sync"
if ([string]::IsNullOrWhiteSpace($SOURCE_BRANCH)) {
    Write-Host "❌ Branch name cannot be empty." -ForegroundColor Red
    exit 1
}

# === Step 1: 检查远程分支是否存在 ===
Run-Git "ls-remote --exit-code --heads origin $SOURCE_BRANCH" "❌ Remote branch 'origin/$SOURCE_BRANCH' does not exist."
Write-Host "✅ Remote branch 'origin/$SOURCE_BRANCH' exists." -ForegroundColor Green

# === Step 2: 定义主分支与备份分支名 ===
$MAIN_BRANCH = "master"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BACKUP_BRANCH = "$MAIN_BRANCH-backup-$timestamp"

# === Step 3: 同步操作 ===
Write-Host "`n🚀 [1/5] Fetching origin..."
Run-Git "fetch origin" "❌ Failed to fetch origin."

Write-Host "📦 [2/5] Checking out $MAIN_BRANCH and creating backup: $BACKUP_BRANCH"
Run-Git "checkout $MAIN_BRANCH" "❌ Failed to checkout $MAIN_BRANCH"
Run-Git "branch $BACKUP_BRANCH" "❌ Failed to create backup branch"

Write-Host "🔄 [3/5] Resetting $MAIN_BRANCH to origin/$SOURCE_BRANCH"
Run-Git "fetch origin $SOURCE_BRANCH" "❌ Failed to fetch source branch"
Run-Git "reset --hard origin/$SOURCE_BRANCH" "❌ Failed to reset $MAIN_BRANCH"

Write-Host "☁️ [4/5] Force pushing $MAIN_BRANCH to origin"
Run-Git "push -f origin $MAIN_BRANCH" "❌ Failed to force push $MAIN_BRANCH to origin"

Write-Host "`n✅ [5/5] Done!" -ForegroundColor Green
Write-Host "🌿 '$MAIN_BRANCH' is now synced to 'origin/$SOURCE_BRANCH'"
Write-Host "📂 Local backup created: $BACKUP_BRANCH" -ForegroundColor Yellow
