#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Path (Resolve-Path $MyInvocation.MyCommand.Path) -Parent

$basicPs1 = Join-Path $scriptDir "run-e2e-test-cases.ps1"
$basicSh = Join-Path $scriptDir "run-e2e-test-cases.sh"
if (Test-Path $basicPs1) {
    & $basicPs1 @args
} elseif (Test-Path $basicSh) {
    & bash "$basicSh" @args
} else {
    Write-Error "basic E2E script not found (expected $basicPs1 or $basicSh)"
    exit 1
}
$basicExit = $LASTEXITCODE
if ($basicExit -ne 0) {
    exit $basicExit
}

$agentsPs1 = Join-Path $scriptDir "run-e2e-agents.ps1"
$agentsSh = Join-Path $scriptDir "run-e2e-agents.sh"
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
