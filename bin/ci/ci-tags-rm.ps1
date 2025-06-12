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

function Find-AppHome {
    $dir = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
    while ($null -ne $dir -and !(Test-Path "$dir/VERSION")) {
        $dir = $dir.Parent
    }
    if (!$dir) {
        throw "VERSION file not found in any parent directory."
    }
    return $dir
}

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

# MAIN EXECUTION
$AppHome = Find-AppHome
Set-Location $AppHome
Confirm-And-Remove-CITags -pattern $pattern -remote $remote