#!/usr/bin/env pwsh

param(
    [string]$remote = "origin",
    [string]$pattern = "v[0-9]+.[0-9]+.[0-9]+-ci.[0-9]+"
)

function Find-VersionFileDirectory {
    $dir = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
    while ($null -ne $dir -and !(Test-Path "$dir/VERSION")) {
        $dir = $dir.Parent
    }
    return $dir
}

function Get-Version {
    param([string]$versionFile)
    if (!(Test-Path $versionFile)) {
        throw "VERSION file not found at $versionFile"
    }
    $version = (Get-Content $versionFile | Out-String).Trim()
    return $version -replace "-SNAPSHOT$"
}

function Get-LatestCITag {
    param([string]$pattern)
    $tags = git tag --list | Where-Object { $_ -match "^$pattern$" }
    if (!$tags) { return $null }
    return $tags | Sort-Object {
        if ($_ -match "^v(\d+)\.(\d+)\.(\d+)-ci\.(\d+)$") {
            [int]$matches[1]*1000000 + [int]$matches[2]*1000 + [int]$matches[3]*10 + [int]$matches[4]
        } else { 0 }
    } -Descending | Select-Object -First 1
}

function Create-And-Push-Tag {
    param(
        [string]$tag,
        [string]$remote
    )
    git tag $tag
    git push $remote $tag
    Write-Host "Created and pushed tag '$tag' to remote '$remote'."
    return $tag
}

# Main
$AppHome = Find-VersionFileDirectory
if (!$AppHome) { Write-Error "VERSION file not found in any parent directory."; exit 1 }
Set-Location $AppHome

$latestTag = Get-LatestCITag $pattern
if (-not $latestTag) {
    $version = Get-Version "$AppHome/VERSION"
    $newTag = "$version-ci.1"
    Write-Host "No existing tags found. Creating new tag: $newTag"
    Create-And-Push-Tag $newTag $remote
    Write-Output $newTag
    exit 0
}

Write-Host "Latest tag found: $latestTag"
if ($latestTag -match "^(v\d+\.\d+\.\d+)-ci\.(\d+)$") {
    $baseVersion = $matches[1]
    $ciNumber = [int]$matches[2]
    $newTag = "$baseVersion-ci." + ($ciNumber + 1)
    Create-And-Push-Tag $newTag $remote
    Write-Output $newTag
} else {
    Write-Error "Latest tag '$latestTag' does not match expected pattern."
    exit 1
}