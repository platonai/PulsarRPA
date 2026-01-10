#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Build Browser4 as a Windows executable (.exe).

.DESCRIPTION
    This script builds the browser4-agents module and generates a Windows executable
    using the launch4j-maven-plugin. The resulting Browser4.exe can be run on Windows
    systems with a Java Runtime Environment (JRE) installed.

.PARAMETER Clean
    Perform a clean build before packaging.

.PARAMETER Test
    Run tests during the build (by default tests are skipped).

.EXAMPLE
    .\build-exe.ps1
    Builds the Windows executable without cleaning or running tests.

.EXAMPLE
    .\build-exe.ps1 -Clean
    Performs a clean build of the Windows executable.

.EXAMPLE
    .\build-exe.ps1 -Clean -Test
    Performs a clean build with tests.
#>

param(
    [switch]$Clean,
    [switch]$Test
)

$ErrorActionPreference = "Stop"

# Find the first parent directory containing the VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}

if (-not (Test-Path "$AppHome/VERSION")) {
    Write-Error "Could not find project root (VERSION file not found)"
    exit 1
}

Set-Location $AppHome

# Maven command
$MvnCmd = Join-Path $AppHome '.\mvnw.cmd'
if (-not (Test-Path $MvnCmd)) {
    $MvnCmd = Join-Path $AppHome '.\mvnw'
}

# Build Maven options
$MvnOptions = @()

if ($Clean) {
    $MvnOptions += 'clean'
}

$MvnOptions += 'package'
$MvnOptions += '-pl'
$MvnOptions += 'browser4/browser4-agents'
$MvnOptions += '-am'
$MvnOptions += '-Pwin-exe'

if (-not $Test) {
    $MvnOptions += '-DskipTests'
}

Write-Host "Building Browser4 Windows executable..."
Write-Host "Command: $MvnCmd $($MvnOptions -join ' ')"
Write-Host ""

# Execute Maven build
& $MvnCmd @MvnOptions

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

# Check if the executable was created
$ExePath = Join-Path $AppHome "browser4\browser4-agents\target\Browser4.exe"
if (Test-Path $ExePath) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "Windows executable created at:" -ForegroundColor Green
    Write-Host "  $ExePath" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "To run Browser4:" -ForegroundColor Yellow
    Write-Host "  .\browser4\browser4-agents\target\Browser4.exe" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Note: Java Runtime Environment (JRE) $((Get-Content "$AppHome\browser4\browser4-agents\pom.xml" | Select-String 'kotlin.compiler.jvmTarget').Line.Trim() -replace '.*>(\d+)<.*', '$1')+ is required to run the executable." -ForegroundColor Yellow
} else {
    Write-Warning "Build completed but Browser4.exe was not found at expected location: $ExePath"
}
