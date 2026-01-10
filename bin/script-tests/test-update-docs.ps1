#!/usr/bin/env pwsh
# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Replace SNAPSHOT version with the release version
@('README.md', 'README.zh.md') | ForEach-Object {
  Get-ChildItem -Path "$AppHome" -Depth 5 -Filter $_ -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace "Browser4", "Browser4Test" | Set-Content $_.FullName
  }
}
