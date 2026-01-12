#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Path (Resolve-Path $MyInvocation.MyCommand.Path) -Parent
$runLegacy = $false
if ($env:RUN_LEGACY_E2E -and $env:RUN_LEGACY_E2E -eq "true") {
    $runLegacy = $true
}

if ($runLegacy) {
    $legacyPs1 = Join-Path $scriptDir "run-e2e-test-legacy.ps1"
    $legacySh = Join-Path $scriptDir "run-e2e-test-legacy.sh"
    if (Test-Path $legacyPs1) {
        & $legacyPs1 @args
    } elseif (Test-Path $legacySh) {
        & bash "$legacySh" @args
    } else {
        Write-Error "Legacy E2E script not found (expected $legacyPs1 or $legacySh)"
        exit 1
    }
    $legacyExit = $LASTEXITCODE
    if ($legacyExit -ne 0) {
        exit $legacyExit
    }
}

$agentsPs1 = Join-Path $scriptDir "run-e2e-fast-agents.ps1"
$agentsSh = Join-Path $scriptDir "run-e2e-fast-agents.sh"
if (Test-Path $agentsPs1) {
    & $agentsPs1 @args
    $exitCode = $LASTEXITCODE
    exit $exitCode
} elseif (Test-Path $agentsSh) {
    & bash "$agentsSh" @args
    $exitCode = $LASTEXITCODE
    exit $exitCode
} else {
    Write-Error "Agents E2E script not found (expected $agentsPs1 or $agentsSh)"
    exit 1
}
