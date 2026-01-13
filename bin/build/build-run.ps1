#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

# Find the first parent directory that contains a VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and -not (Test-Path (Join-Path $AppHome "VERSION"))) {
  $AppHome = Split-Path -Parent $AppHome
}

if (-not $AppHome -or -not (Test-Path (Join-Path $AppHome "VERSION"))) {
  throw "VERSION file not found when resolving project root."
}

Set-Location $AppHome

& (Join-Path $AppHome "bin/build/build.ps1") @args

& (Join-Path $AppHome "bin/browser4.ps1") @args
