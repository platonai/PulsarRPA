#!/usr/bin/env pwsh

<#
.SYNOPSIS
  Interactively remove CI tags matching a specified pattern from both local and remote.

.DESCRIPTION
  This script finds all git tags matching a versioned CI tag pattern (e.g., v1.2.3-ci.4), then asks for confirmation before deleting each tag locally and remotely.
#>

param(
    [string]$remote = "origin",
    [string]$pattern = "v[0-9]+.[0-9]+.[0-9]+-ci.[0-9]+"
)

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

function Get-MatchingTags {
    param([string]$pattern)
    return git tag --list | Where-Object { $_ -match "^$pattern$" }
}

function Remove-Tag {
    param(
        [string]$tag,
        [string]$remote
    )
    Write-Host "Removing tag '$tag' locally."
    git tag -d $tag | Out-Null
    Write-Host "Removing tag '$tag' from remote '$remote'."
    git push $remote --delete $tag | Out-Null
}

function Confirm-And-Remove-CITags {
    param(
        [string]$pattern,
        [string]$remote
    )
    $tags = Get-MatchingTags $pattern
    if ($tags) {
        Write-Host "Found CI tags to potentially remove: $($tags -join ', ')"
        foreach ($tag in $tags) {
            $confirm = Read-Host "Do you want to remove tag '$tag' from local and '$remote'? (y/N)"
            if ($confirm -match '^(y|yes)$') {
                Remove-Tag $tag $remote
            } else {
                Write-Host "Skipped removing tag '$tag'."
            }
        }
    } else {
        Write-Host "No matching tags found to remove."
    }
}

Confirm-And-Remove-CITags -pattern $pattern -remote $remote