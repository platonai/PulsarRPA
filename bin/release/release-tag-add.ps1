#!/usr/bin/env pwsh

param(
    [string]$remote = "origin",
    [string]$pattern = "v[0-9]+.[0-9]+.[0-9]+"
)

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Add release tag
# - Make sure we are on the main/master branch
# - Check if the VERSION file exists
# - If it exists, read the version from it
# - If it does not exist, exit with an error
# - If the version is a snapshot, remove the "-SNAPSHOT" suffix
# - Create a new tag with the format "vX.Y.Z" where X.Y.Z is the version from the VERSION file
# - You should ask the user to confirm before creating the tag

if (-not (git rev-parse --abbrev-ref HEAD | Where-Object { $_ -in @("main", "master") })) {
    Write-Error "You must be on the main or master branch to create a release tag."
    exit 1
}

if (-not (Test-Path "VERSION")) {
    Write-Error "VERSION file does not exist. Cannot create release tag."
    exit 1
}

$version = Get-Content "VERSION" | Out-String
$version = $version.Trim()
if ($version.EndsWith("-SNAPSHOT")) {
    $version = $version.Substring(0, $version.Length - 9)
}

$newTag = "v$version"
if (git tag --list | Where-Object { $_ -eq $newTag }) {
    Write-Host "Tag '$newTag' already exists. No new tag created."
} else {
    # Create the new tag if the user confirms
    $confirmation = Read-Host "Do you want to create the tag '$newTag'? (y/n)"
    if ($confirmation -eq 'y' -or $confirmation -eq 'Y') {
        git tag $newTag
        git push $remote $newTag
        Write-Host "Created new tag '$newTag' and pushed it to remote '$remote'."
        Write-Output $newTag
    } else {
        Write-Host "Tag creation cancelled."
    }
}
