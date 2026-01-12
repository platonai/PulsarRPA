#!/usr/bin/env pwsh

<#
    run-e2e-agents.ps1 - PowerShell version of run-e2e-agents.sh
#>

param(
    [string]$Url,
    [string]$Test,
    [int]$Count,
    [string]$Order,
    [string]$Level,
    [int]$TaskTimeout,
    [int]$TotalTimeout,
    [switch]$SkipServer,
    [switch]$Verbose,
    [switch]$Help
)

if ($Help) {
    @"
Usage: run-e2e-agents.ps1 [OPTIONS]

OPTIONS:
    -Url <URL>             Browser4 base URL (default: http://localhost:8182)
    -Test <SELECTION>      Test selection (comma-separated numbers or "all", default: all)
    -Count <N>             Number of tests to run after selection (default: all)
    -Order <MODE>          Execution order: random | sequential (default: random)
    -Level <LEVELS>        Comma-separated task levels to run (default: Simple; use "all" for all levels)
    -TaskTimeout <SEC>     Per-test timeout in seconds (default: 180)
    -TotalTimeout <SEC>    Total suite timeout in seconds (default: 1200)
    -SkipServer            Skip server connectivity check
    -Verbose               Enable verbose output
    -Help                  Show this help message
"@
    exit 0
}

$ErrorActionPreference = "Stop"

function Get-AppHome {
    $dir = Split-Path -Parent $PSCommandPath
    while ($true) {
        if (Test-Path (Join-Path $dir "VERSION")) { return $dir }
        $parent = Split-Path -Parent $dir
        if ($parent -eq $dir) { break }
        $dir = $parent
    }
    return (Get-Location).Path
}

$AppHome = Get-AppHome
Set-Location $AppHome

# Configuration (params override env, then defaults)
$API_BASE = if ($Url) { $Url } elseif ($env:API_BASE) { $env:API_BASE } else { "http://localhost:8182" }
$COMMAND_ENDPOINT = "$API_BASE/api/commands/plain?mode=async"
$COMMAND_STATUS_BASE = "$API_BASE/api/commands"
$USE_CASES_DIR = Join-Path $AppHome "bin/tests/use-cases"
$TEST_RESULTS_DIR = Join-Path $AppHome "target/test-results/use-cases"
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$LOG_FILE = Join-Path $TEST_RESULTS_DIR "use_case_tests_$TIMESTAMP.log"

$TASK_TIMEOUT = if ($PSBoundParameters.ContainsKey('TaskTimeout')) { $TaskTimeout } elseif ($env:TASK_TIMEOUT) { $env:TASK_TIMEOUT } else { 180 }
$TOTAL_TIMEOUT = if ($PSBoundParameters.ContainsKey('TotalTimeout')) { $TotalTimeout } elseif ($env:TOTAL_TIMEOUT) { $env:TOTAL_TIMEOUT } else { 1200 }
$MIN_SUCCESS_RATE = if ($env:MIN_SUCCESS_RATE) { $env:MIN_SUCCESS_RATE } else { 50 }
$TEST_SELECTION = if ($PSBoundParameters.ContainsKey('Test')) { $Test } elseif ($env:TEST_SELECTION) { $env:TEST_SELECTION } else { "all" }
$TEST_COUNT = if ($PSBoundParameters.ContainsKey('Count')) { $Count } elseif ($env:TEST_COUNT) { $env:TEST_COUNT } else { 0 }
$EXECUTION_ORDER = if ($PSBoundParameters.ContainsKey('Order')) { $Order } elseif ($env:EXECUTION_ORDER) { $env:EXECUTION_ORDER } else { "random" }
$VERBOSE_MODE = if ($Verbose) { $true } elseif ($env:VERBOSE -eq "true") { $true } else { $false }
$SKIP_SERVER_CHECK = if ($SkipServer) { $true } elseif ($env:SKIP_SERVER_CHECK -eq "true") { $true } else { $false }
$LEVEL_FILTER = if ($PSBoundParameters.ContainsKey('Level')) { $Level } elseif ($env:LEVEL_FILTER) { $env:LEVEL_FILTER } else { "Simple" }
$DEFAULT_TASK_NOTE = "If you exceed either 30 steps or 10 minutes, you must complete the task immediately."

# Counters
$TOTAL_TESTS = 0
$EXECUTED_TESTS = 0
$PASSED_TESTS = 0
$FAILED_TESTS = 0
$SKIPPED_TESTS = 0
$TIMED_OUT_TESTS = 0
$LEVEL_FILTERS = @()

function Log($msg) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[{0}] {1}" -f $timestamp, $msg
    Write-Output $line
    Add-Content -Path $LOG_FILE -Value $line
}

function VLog($msg) {
    if ($VERBOSE_MODE) { Log "[VERBOSE] $msg" }
}

function Ensure-Directories {
    New-Item -ItemType Directory -Path $TEST_RESULTS_DIR -Force | Out-Null
}

function Extract-Description($file) {
    Get-Content $file | Where-Object { $_ -match '^\s*#' } | ForEach-Object { $_ -replace '^\s*#\s*', '' } | Select-Object -First 5
}

function Extract-Level($file) {
    $level = Get-Content $file | Where-Object { $_ -match '^\s*#\s*Level:' } | Select-Object -First 1
    if ($level) { return ($level -replace '^\s*#\s*Level:\s*', '') }
    return "Simple"
}

function Extract-TaskContent($file) {
    Get-Content $file | Where-Object { $_ -notmatch '^\s*#' -and $_ -notmatch '^\s*$' }
}

function Assert-HttpSuccess($statusCode) {
    return ($statusCode -ge 200 -and $statusCode -lt 400)
}

function Record-IfChanged([string]$content, [string]$file, [ref]$lastValue, [string]$label) {
    if ($content -ne $lastValue.Value) {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        $prefix = if ([string]::IsNullOrWhiteSpace($label)) { "" } else { "[{0}] " -f $label }
        Add-Content -Path $file -Value "[${timestamp}] ${prefix}${content}"
        $lastValue.Value = $content
        return $true
    }
    return $false
}

function Level-MatchesFilter($level) {
    $lvl = $level.ToLower()
    foreach ($lf in $LEVEL_FILTERS) {
        if ($lf -eq "all") { return $true }
        if ($lvl -eq $lf) { return $true }
    }
    return $false
}

function Shuffle-Array([array]$items) {
    $items | Get-Random -Count $items.Count
}

function Check-Server {
    Log "[INFO] Checking Browser4 server at $API_BASE..."
    try {
        $health = Invoke-WebRequest -Uri "$API_BASE/actuator/health" -TimeoutSec 10 -UseBasicParsing
        if (Assert-HttpSuccess $health.StatusCode) {
            Log "[SUCCESS] Browser4 server is healthy and responding"
            return $true
        }
    } catch { }
    try {
        $root = Invoke-WebRequest -Uri "$API_BASE/" -TimeoutSec 10 -UseBasicParsing
        if (Assert-HttpSuccess $root.StatusCode) {
            Log "[WARNING] Server responding but health check endpoint unavailable"
            return $true
        }
    } catch { }
    Log "[ERROR] Browser4 server not accessible at $API_BASE"
    Log "[HINT] Start Browser4 with: java -DOPENROUTER_API_KEY=`$OPENROUTER_API_KEY -jar Browser4.jar"
    return $false
}

function Run-UseCaseTest($useCaseFile, $testNumber, $totalTests) {
    $global:TOTAL_TESTS++

    $filename = Split-Path $useCaseFile -Leaf
    $testName = $filename -replace '\.txt$', ''

    Log ""
    Log "===================================================================="
    Log "[TEST $testNumber/$totalTests] $testName"
    Log "--------------------------------------------------------------------"

    $description = Extract-Description $useCaseFile
    $level = Extract-Level $useCaseFile
    $taskContent = (Extract-TaskContent $useCaseFile) -join "`n"
    $taskPayload = if ($DEFAULT_TASK_NOTE) { "$taskContent`n`n$DEFAULT_TASK_NOTE" } else { $taskContent }
    $timeout = [int]$TASK_TIMEOUT

    Log "[DESCRIPTION]"
    foreach ($line in $description) { Log "    $line" }
    Log "[LEVEL] $level"
    Log "[TIMEOUT] ${timeout}s"
    if ($VERBOSE_MODE) {
        Log "[TASK CONTENT]"
        foreach ($line in $taskPayload -split "`n") { Log "    $line" }
    }

    $statusFile = Join-Path $TEST_RESULTS_DIR "${testName}_status.log"
    $resultFile = Join-Path $TEST_RESULTS_DIR "${testName}_result.json"
    Set-Content -Path $statusFile -Value "" -NoNewline
    $lastStatus = [ref] ""

    $startEpoch = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    Log "[INFO] Start time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')"

    $submitSuccess = $false
    $commandId = $null
    try {
        $resp = Invoke-WebRequest -Uri $COMMAND_ENDPOINT -Method Post -Body $taskPayload -ContentType "text/plain" -TimeoutSec 15 -UseBasicParsing
        $submitSuccess = $true
        $commandId = $resp.Content.Trim('"', "`r", "`n")
        Record-IfChanged $resp.Content $statusFile $lastStatus "submit" | Out-Null
        $submitHttp = $resp.StatusCode
        if (-not $commandId -or -not (Assert-HttpSuccess $submitHttp)) {
            throw "Invalid response from server (HTTP: $submitHttp, id: '$commandId')"
        }
    } catch {
        $global:FAILED_TESTS++
        $global:EXECUTED_TESTS++
        Log "[FAIL] Failed to submit command: $_"
        Log "[INFO] End time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K') (Duration: $([int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - $startEpoch)s)"
        Log "===================================================================="
        return $false
    }

    Log "[INFO] Command ID: $commandId"
    $statusUrl = "$COMMAND_STATUS_BASE/$commandId/status"
    $resultUrl = "$COMMAND_STATUS_BASE/$commandId/result"

    $lastChangeEpoch = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $staleIntervals = 0
    $testPassed = $false
    $testResult = ""

    while ($true) {
        $nowEpoch = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        if ($nowEpoch - $startEpoch -ge $timeout) {
            $global:TIMED_OUT_TESTS++
            $global:EXECUTED_TESTS++
            $testResult = "TIMEOUT"
            Log "[TIMEOUT] Task exceeded per-test timeout (${timeout}s); aborting"
            break
        }
        $statusResponse = $null
        try {
            $statusResponse = Invoke-WebRequest -Uri $statusUrl -TimeoutSec $timeout -UseBasicParsing
        } catch {
            $global:FAILED_TESTS++
            $global:EXECUTED_TESTS++
            $testResult = "FAIL"
            Log "[FAIL] Failed to query status: $_"
            break
        }

        $statusBody = $statusResponse.Content
        $statusHttp = $statusResponse.StatusCode

        if (Record-IfChanged $statusBody $statusFile $lastStatus "status") {
            $lastChangeEpoch = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
            $staleIntervals = 0
        } else {
            if ($nowEpoch - $lastChangeEpoch -ge 30) {
                $staleIntervals++
                $lastChangeEpoch = $nowEpoch
                Log "[WAIT] Status unchanged for $($staleIntervals * 30)s (id: $commandId)"
            }
        }

        if (-not (Assert-HttpSuccess $statusHttp)) {
            $global:FAILED_TESTS++
            $global:EXECUTED_TESTS++
            $testResult = "FAIL"
            Log "[FAIL] Status HTTP error: $statusHttp"
            break
        }

        if ($statusBody -match 'isDone.*true' -or $statusBody -match 'done.*true') {
            Log "[INFO] Task completed, fetching result..."
            try {
                $resultResp = Invoke-WebRequest -Uri $resultUrl -TimeoutSec $timeout -UseBasicParsing
                $resultBody = $resultResp.Content
                Record-IfChanged $resultBody $statusFile $lastStatus "result" | Out-Null
                if (Assert-HttpSuccess $resultResp.StatusCode) {
                    Set-Content -Path $resultFile -Value $resultBody
                    $global:PASSED_TESTS++
                    $global:EXECUTED_TESTS++
                    $testResult = "PASS"
                    $testPassed = $true
                    Log "[PASS] Result saved to $resultFile"
                } else {
                    throw "Result HTTP error: $($resultResp.StatusCode)"
                }
            } catch {
                $global:FAILED_TESTS++
                $global:EXECUTED_TESTS++
                $testResult = "FAIL"
                Log "[FAIL] Failed to fetch result: $_"
            }
            break
        }

        if ($staleIntervals -ge 3) {
            $global:TIMED_OUT_TESTS++
            $global:EXECUTED_TESTS++
            $testResult = "TIMEOUT"
            Log "[TIMEOUT] Status not updated for 90 seconds, marking as timeout"
            break
        }

        Start-Sleep -Seconds 1
    }

    $endEpoch = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $duration = $endEpoch - $startEpoch
    Log "[INFO] End time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K') (Duration: ${duration}s)"
    Log "===================================================================="

    return $testPassed
}

function Print-Summary {
    $successRate = 0
    if ($EXECUTED_TESTS -gt 0) { $successRate = [int](($PASSED_TESTS * 100) / $EXECUTED_TESTS) }

    Log ""
    Log "===================================================================="
    Log "[FINAL SUMMARY] Use Case Test Results"
    Log "===================================================================="
    Log "Test Session: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    Log "Server: $API_BASE"
    Log "Use Cases Directory: $USE_CASES_DIR"
    Log "--------------------------------------------------------------------"
    Log "Total Tests: $TOTAL_TESTS"
    Log "Executed: $EXECUTED_TESTS"
    Log "Passed: $PASSED_TESTS"
    Log "Failed: $FAILED_TESTS"
    Log "Timed Out: $TIMED_OUT_TESTS"
    Log "Skipped: $SKIPPED_TESTS"
    Log "Success Rate: ${successRate}% (minimum required: ${MIN_SUCCESS_RATE}%)"
    Log "Log File: $LOG_FILE"
    Log "Results Directory: $TEST_RESULTS_DIR"
    Log "===================================================================="

    if ($EXECUTED_TESTS -eq 0) { exit 1 }
    if ($successRate -ge [int]$MIN_SUCCESS_RATE) { exit 0 } else { exit 1 }
}

Ensure-Directories

Log "[INFO] Browser4 Use Case Test Suite"
Log "[INFO] Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Log "[INFO] Server URL: $API_BASE"
Log "[INFO] Verbose Mode: $VERBOSE_MODE"
Log "[INFO] Level filter: $LEVEL_FILTER"

if (-not (Get-Command curl -ErrorAction SilentlyContinue)) {
    # curl may be alias to Invoke-WebRequest; ensure available
    if (-not (Get-Command Invoke-WebRequest -ErrorAction SilentlyContinue)) {
        Log "[ERROR] Neither curl nor Invoke-WebRequest is available."
        exit 1
    }
}

if (-not $SKIP_SERVER_CHECK) {
    if (-not (Check-Server)) {
        Log "[WARNING] Use -SkipServer to bypass server check"
        exit 1
    }
}

if (-not (Test-Path $USE_CASES_DIR)) {
    Log "[ERROR] Use cases directory not found: $USE_CASES_DIR"
    exit 1
}

$LEVEL_FILTERS = @()
foreach ($lf in ($LEVEL_FILTER -split ",")) {
    $val = ($lf -replace "\s", "").ToLower()
    if ($val) { $LEVEL_FILTERS += $val }
}
if ($LEVEL_FILTERS.Count -eq 0) { $LEVEL_FILTERS = @("simple") }

$useCaseFiles = Get-ChildItem -Path $USE_CASES_DIR -Filter *.txt -File -Recurse | Sort-Object FullName
if (-not $useCaseFiles) {
    Log "[ERROR] No use case files found in $USE_CASES_DIR"
    exit 1
}
Log "[INFO] Found $($useCaseFiles.Count) use case files"

$testsToRun = @()
if ($TEST_SELECTION -eq "all") {
    $testsToRun = $useCaseFiles
} else {
    foreach ($num in $TEST_SELECTION -split ",") {
        $n = $num.Trim()
        $match = $useCaseFiles | Where-Object { $_.Name -like "$n*" } | Select-Object -First 1
        if ($match) { $testsToRun += $match }
    }
}

# filter by level
$filtered = @()
foreach ($file in $testsToRun) {
    $lvl = Extract-Level $file.FullName
    if (Level-MatchesFilter $lvl) {
        $filtered += $file
    } else {
        $global:SKIPPED_TESTS++
        VLog "[SKIP] Skipping $($file.Name) due to level '$lvl'"
    }
}
$testsToRun = $filtered

$totalSelected = $testsToRun.Count
$countLabel = if ($TEST_COUNT -eq 0) { "all" } else { $TEST_COUNT }
Log "[INFO] Tests selected: $totalSelected | Order: $EXECUTION_ORDER | Count: $countLabel"

if ($TEST_COUNT -lt 0) {
    Log "[ERROR] TEST_COUNT must be non-negative (got: $TEST_COUNT)"
    exit 1
}

if ($EXECUTION_ORDER -eq "random") {
    $testsToRun = @(Shuffle-Array $testsToRun)
}

if ($TEST_COUNT -gt 0 -and $testsToRun.Count -gt $TEST_COUNT) {
    $testsToRun = $testsToRun[0..($TEST_COUNT-1)]
}

$totalSelected = $testsToRun.Count
Log "[INFO] Tests to execute after limiting: $totalSelected"

$suiteStart = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$counter = 0
foreach ($file in $testsToRun) {
    $now = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    if ($now - $suiteStart -ge [int]$TOTAL_TIMEOUT) {
        Log "[TIMEOUT] Suite timeout (${TOTAL_TIMEOUT}s) reached; skipping remaining tests"
        break
    }
    $counter++
    $ok = Run-UseCaseTest $file.FullName $counter $totalSelected
    if (-not $ok) {
        Log "[INFO] Continuing to next test despite failure"
    }
}

Print-Summary
