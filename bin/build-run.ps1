#!/usr/bin/env pwsh

# Call bin/build/build-run.ps1 with all passed arguments

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BuildRunScript = Join-Path $ScriptDir 'build-run.ps1'
& $BuildRunScript @args
