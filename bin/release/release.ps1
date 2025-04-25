# 🚀 PulsarRPA Release Script
# This script orchestrates the release process by executing git and docker release scripts
# Usage: .\release.ps1 [-Verbose]

# 🔍 Find the first parent directory containing the VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($null -ne $AppHome -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = $AppHome.Parent
}

if ($null -eq $AppHome) {
    Write-Error "❌ VERSION file not found in any parent directory"
    exit 1
}

Write-Verbose "📂 Found project root at: $AppHome"
Set-Location $AppHome

# 📦 Define script paths
$Bin = "$AppHome\bin"
$GitReleaseScript = "$Bin\release\git-release.ps1"
$DockerReleaseScript = "$Bin\release\docker-release.ps1"
$AssetReleaseScript = "$Bin\release\asset-release.ps1"

# 🔍 Verify required scripts exist
if (-not (Test-Path $GitReleaseScript)) {
    Write-Error "❌ Git release script not found: $GitReleaseScript"
    exit 1
}

if (-not (Test-Path $DockerReleaseScript)) {
    Write-Error "❌ Docker release script not found: $DockerReleaseScript"
    exit 1
}

# 🚀 Execute release scripts
try {
    Write-Verbose "📦 Starting git release process..."
    & $GitReleaseScript
    if ($LASTEXITCODE -ne 0) {
        throw "Git release failed with exit code $LASTEXITCODE"
    }

    Write-Verbose "🐳 Starting docker release process..."
    & $DockerReleaseScript
    if ($LASTEXITCODE -ne 0) {
        throw "Docker release failed with exit code $LASTEXITCODE"
    }

    Write-Verbose "📦 Starting asset release process..."
    & $AssetReleaseScript
    if ($LASTEXITCODE -ne 0) {
        throw "Asset release failed with exit code $LASTEXITCODE"
    }

    Write-Host "✅ Release process completed successfully!" -ForegroundColor Green
}
catch {
    Write-Error "❌ Release process failed: $_"
    exit 1
}
