#!/usr/bin/env pwsh

echo "This script is deprecated, please use 'browser4.ps1' command instead."

$ErrorActionPreference = "Stop"

# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

$UBERJAR = Join-Path $PWD "target\Browser4.jar"
if (-not (Test-Path $UBERJAR)) {
    $SERVER_HOME = Join-Path $AppHome "pulsar-app\pulsar-browser4"
    Copy-Item (Join-Path $SERVER_HOME "target\Browser4.jar") -Destination $UBERJAR
}

# Other Java options
$JAVA_OPTS = "$JAVA_OPTS -Dfile.encoding=UTF-8" # Use UTF-8

Write-Host "Using these JAVA_OPTS: $JAVA_OPTS"

Start-Process -NoNewWindow -Wait -FilePath "java" -ArgumentList ("$JAVA_OPTS", "-jar", "$UBERJAR")
