#!/usr/bin/env pwsh

param(
    [string]$PreReleaseVersion = "ci",
    [string]$remote = "origin"
)

# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Get version information
$SNAPSHOT_VERSION = Get-Content "$AppHome\VERSION" -TotalCount 1
$version = $SNAPSHOT_VERSION -replace "-SNAPSHOT", ""

$parts = $version -split "\."
$PREFIX = $parts[0] + "." + $parts[1]
$pattern = "^v$version-$PreReleaseVersion\.[0-9]+$"

# Get all matching tags and sort them by version and pre-release number
$tags = git tag --list | Where-Object { $_ -match "^$pattern$" }
if (-not $tags) {
    $newTag = "v$version-$PreReleaseVersion.1"
    Write-Host "No existing tags found. Creating new tag: $newTag"
    git tag $newTag
    git push $remote $newTag
    Write-Host "Created new tag '$newTag' and pushed it to remote '$remote'."
    Write-Output $newTag
    exit 0
}

# Sort tags by version and pre-release number (natural order)
$latestTag = $tags | Sort-Object {
    # Extract version and pre-release number for sorting
    if ($_ -match "^v(\d+)\.(\d+)\.(\d+)-$PreReleaseVersion\.(\d+)$") {
        return [int]$matches[1]*1000000 + [int]$matches[2]*10000 + [int]$matches[3]*100 + [int]$matches[4]
    }
    return 0
} -Descending | Select-Object -First 1

Write-Host "Latest tag found: $latestTag"

if ($latestTag -match "^(v\d+\.\d+\.\d+)-$PreReleaseVersion\.(\d+)$") {
    $baseVersion = $matches[1]
    $prNumber = [int]$matches[2]
    $newPrNumber = $prNumber + 1
    $newTag = "$baseVersion-$PreReleaseVersion.$newPrNumber"
    git tag $newTag
    git push $remote $newTag
    Write-Host "Created new tag '$newTag' and pushed it to remote '$remote'."
    Write-Output $newTag
} else {
    Write-Error "Latest tag $latestTag does not match expected pattern."
    exit 1
}
