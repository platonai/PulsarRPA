#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

Write-Host "Fetching latest remote branches..."
git fetch origin

Write-Host "Checking out main branch..."
git checkout main

Write-Host "Resetting main branch to match origin/master..."
git reset --hard origin/master

Write-Host "Force pushing main branch to remote..."
git push origin main --force

Write-Host "Main branch is now fully synchronized with master."
