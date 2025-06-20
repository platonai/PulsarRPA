#!/usr/bin/env pwsh

param(
    [string]$remote = "origin"
)

# Find VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

if (!$AppHome) {
    Write-Error "VERSION file not found"
    exit 1
}

Set-Location $AppHome
Write-Host "Working in: $AppHome"

# Check if we're in a git repo
if (!(Test-Path ".git")) {
    Write-Error "Not a git repository"
    exit 1
}

# Check current branch
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -notin @("main", "master")) {
    Write-Error "Must be on main/master branch. Current: $branch"
    exit 1
}

Write-Host "Current branch: $branch"

# Check for uncommitted changes
$status = git status --porcelain
if ($status) {
    Write-Warning "Uncommitted changes detected"
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne 'y') {
        Write-Host "Cancelled"
        exit 0
    }
}

# Read and process version
if (!(Test-Path "VERSION")) {
    Write-Error "VERSION file not found"
    exit 1
}

$version = (Get-Content "VERSION").Trim()
Write-Host "Version from file: $version"

if ($version.EndsWith("-SNAPSHOT")) {
    $version = $version.Replace("-SNAPSHOT", "")
    Write-Host "Cleaned version: $version"
}

# Validate version format
if ($version -notmatch "^\d+\.\d+\.\d+") {
    Write-Error "Invalid version format: $version"
    exit 1
}

$newTag = "v$version"

# Check if tag already exists
$existingTag = git tag -l $newTag
if ($existingTag) {
    Write-Host "Tag '$newTag' already exists"

    $confirm = Read-Host "Do you want to overwrite it? (y/n)"
    if ($confirm -ne 'y') {
        Write-Host "Cancelled"
        exit 0
    }
    try {
        git tag -d $newTag
        Write-Host "Deleted existing tag: $newTag"
    } catch {
        Write-Error "Failed to delete existing tag: $_"
        exit 1
    }
}

# Get previous tag for release notes
$prevTag = git tag --list | Where-Object { $_ -match '^v\d+\.\d+\.\d+$' } |
        Sort-Object { [version]($_ -replace '^v','') } -Descending |
        Select-Object -First 1

if ($prevTag) {
    Write-Host "`nChanges since $prevTag :"
    $changes = git log --oneline --no-merges "$prevTag..HEAD"
    if ($changes) {
        $changes | ForEach-Object { Write-Host "  • $_" }
    } else {
        Write-Host "  No changes"
    }
} else {
    Write-Host "`nRecent commits:"
    git log --oneline --no-merges -5 | ForEach-Object { Write-Host "  • $_" }
}

# Confirm creation
Write-Host ""
$confirm = Read-Host "Create and push tag '$newTag'? (y/n)"
if ($confirm -ne 'y') {
    Write-Host "Cancelled"
    exit 0
}

# Create and push tag
try {
    git tag $newTag
    git push $remote $newTag
    Write-Host "✅ Successfully created and pushed tag: $newTag"

    # Try to show GitHub URL
    $remoteUrl = git config --get remote.$remote.url
    if ($remoteUrl -match 'github\.com[:/](.+?)(?:\.git)?$') {
        $repo = $matches[1]
        Write-Host "🔗 Release URL: https://github.com/$repo/releases/tag/$newTag"
    }

    Write-Output $newTag
} catch {
    Write-Error "Failed to create/push tag: $_"
    exit 1
}