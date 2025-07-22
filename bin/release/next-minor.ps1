#!/usr/bin/env pwsh

# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Make sure we are not at master branch
$currentBranch = git rev-parse --abbrev-ref HEAD
if ($currentBranch -eq 'master')
{
    Write-Host "You are on the master branch. Please switch to a feature branch before running this script."
    exit 1
}

# Get version information
$SNAPSHOT_VERSION = Get-Content "$AppHome\VERSION" -TotalCount 1
$VERSION = $SNAPSHOT_VERSION -replace "-SNAPSHOT", ""

$parts = $VERSION -split "\."
$PREFIX = $parts[0] + "." + $parts[1]
$MINOR_VERSION = [int]$parts[2]
$MINOR_VERSION = $MINOR_VERSION + 1

$NEXT_VERSION = "$PREFIX.$MINOR_VERSION"
$NEXT_SNAPSHOT_VERSION = "$NEXT_VERSION-SNAPSHOT"

# Output new version
Write-Host "New version: $NEXT_SNAPSHOT_VERSION"

# Update VERSION file
$NEXT_SNAPSHOT_VERSION | Set-Content "$AppHome\VERSION"

# Update $AppHome/pom.xml
$pomXmlPath = "$AppHome\pom.xml"
((Get-Content $pomXmlPath) -replace "<tag>v$VERSION</tag>", "<tag>v$NEXT_VERSION</tag>") | Set-Content $pomXmlPath

# Update pom.xml files
Get-ChildItem "$AppHome" -Depth 3 -Filter 'pom.xml' -Recurse | ForEach-Object {
    ((Get-Content $_.FullName) -replace $SNAPSHOT_VERSION, $NEXT_SNAPSHOT_VERSION) | Set-Content $_.FullName
}

# Files containing the version number to upgrade
$VERSION_AWARE_FILES = @(
    "$AppHome\README.md"
    "$AppHome\README-CN.md"
)

# Replace version numbers in files
foreach ($F in $VERSION_AWARE_FILES)
{
    if (Test-Path $F)
    {
        # Replace SNAPSHOT versions
        ((Get-Content $F) -replace $SNAPSHOT_VERSION, $NEXT_SNAPSHOT_VERSION) | Set-Content $F

        # Replace any version numbers with v prefix that match pattern v[0-9]+.[0-9]+.[0-9]+ to v$VERSION
        # Note: The version is not the latest version since the latest version is not released yet.
        ((Get-Content $F) -replace "v[0-9]+\.[0-9]+\.[0-9]+", "v$VERSION") | Set-Content $F
    }
}

# Commit comment
$COMMENT = $NEXT_SNAPSHOT_VERSION -replace "-SNAPSHOT", ""

# Prompt for confirmation
Write-Host "Ready to commit with comment: <$COMMENT>"
$confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
if ($confirm -eq 'Y')
{
    git add .
    git commit -m "$COMMENT"
    git push
}
