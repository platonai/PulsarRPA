#!/usr/bin/env pwsh

param(
    [string]$remote = "origin"
)

# Find the first parent directory containing the VERSION file
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
$pattern = "^v$PREFIX\.[0-9]+-ci\.[0-9]+$"

# Get all matching tags and sort them by version and ci number
$tags = git tag --list | Where-Object { $_ -match "^$pattern$" }
if (-not $tags) {
    $newTag = "v$version-ci.1"
    Write-Host "No existing tags found. Creating new tag: $newTag"
    git tag $newTag
    git push $remote $newTag
    Write-Host "Created new tag '$newTag' and pushed it to remote '$remote'."
    Write-Output $newTag
    exit 0
}

# Sort tags by version and CI number (natural order)
$latestTag = $tags | Sort-Object {
    # Extract version and CI number for sorting
    if ($_ -match "^v(\d+)\.(\d+)\.(\d+)-ci\.(\d+)$") {
        return [int]$matches[1]*1000000 + [int]$matches[2]*10000 + [int]$matches[3]*100 + [int]$matches[4]
    }
    return 0
} -Descending | Select-Object -First 1

Write-Host "Latest tag found: $latestTag"

if ($latestTag -match "^(v\d+\.\d+\.\d+)-ci\.(\d+)$") {
    $baseVersion = $matches[1]
    $ciNumber = [int]$matches[2]
    $newCiNumber = $ciNumber + 1
    $newTag = "$baseVersion-ci.$newCiNumber"
    git tag $newTag
    git push $remote $newTag
    Write-Host "Created new tag '$newTag' and pushed it to remote '$remote'."
    Write-Output $newTag
} else {
    Write-Error "Latest tag $latestTag does not match expected pattern."
    exit 1
}