#!/usr/bin/env pwsh

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

.\bin\build.ps1 $args

$SERVER_HOME = Join-Path $AppHome "pulsar-app\pulsar-master"
Set-Location $SERVER_HOME

../../mvnw spring-boot:run

Set-Location $AppHome
