#!/usr/bin/env pwsh
# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

function Set-TextPreserveNewline {
  param(
    [Parameter(Mandatory)] [string] $Path,
    [Parameter(Mandatory)] [string] $Text,
    [Parameter(Mandatory)] [string] $NewLine
  )

  $normalized = $Text -replace "\r\n|\r|\n", $NewLine
  [System.IO.File]::WriteAllText($Path, $normalized, [System.Text.UTF8Encoding]::new($false))
}

# Replace SNAPSHOT version with the release version
@('README.md', 'README.zh.md') | ForEach-Object {
  Get-ChildItem -Path "$AppHome" -Depth 5 -Filter $_ -Recurse | ForEach-Object {
    $path = $_.FullName
    $original = Get-Content -Path $path -Raw
    $nl = if ($original -match "\r\n") { "`r`n" } else { "`n" }

    $updated = $original -replace "Browser4", "Browser4Test"

    if ($updated -ne $original) {
      Set-TextPreserveNewline -Path $path -Text $updated -NewLine $nl
    }
  }
}
