#!/usr/bin/env pwsh

# Find the first parent directory that contains a VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and -not (Test-Path (Join-Path $AppHome "VERSION"))) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Import common utility script
. $AppHome\bin\common\Util.ps1

Fix-Encoding-UTF8

& (Join-Path $AppHome "bin/build/build.ps1") @args

& (Join-Path $AppHome "bin/browser4.ps1") @args
