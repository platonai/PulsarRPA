#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

# Locate AppHome (parent containing VERSION)
$scriptPath = $MyInvocation.MyCommand.Path
$AppHome = Split-Path -Path (Resolve-Path $scriptPath) -Parent
while (-not (Test-Path (Join-Path $AppHome "VERSION"))) {
    $parent = Split-Path -Path $AppHome -Parent
    if ([string]::IsNullOrEmpty($parent) -or $parent -eq $AppHome) {
        Write-Error "VERSION not found in parent directories of $scriptPath"
        exit 1
    }
    $AppHome = $parent
}
Set-Location $AppHome

# Configuration
$API_BASE = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:8182" }
$COMMAND_ENDPOINT = "$API_BASE/api/commands/plain?mode=async"
$COMMAND_STATUS_BASE = "$API_BASE/api/commands"
$USE_CASES_DIR = Join-Path $AppHome "bin/tests/use-cases"
$TEST_RESULTS_DIR = Join-Path $AppHome "target/test-results/use-cases"
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$LOG_FILE = Join-Path $TEST_RESULTS_DIR "use_case_tests_$TIMESTAMP.log"

function Update-Endpoints {
    $script:COMMAND_ENDPOINT = "$API_BASE/api/commands/plain?mode=async"
    $script:COMMAND_STATUS_BASE = "$API_BASE/api/commands"
}

# Timeout settings (seconds)
$DEFAULT_TIMEOUT = if ($env:DEFAULT_TIMEOUT) { [int]$env:DEFAULT_TIMEOUT } else { 300 }
$SIMPLE_TIMEOUT = if ($env:SIMPLE_TIMEOUT) { [int]$env:SIMPLE_TIMEOUT } else { 180 }
$COMPLEX_TIMEOUT = if ($env:COMPLEX_TIMEOUT) { [int]$env:COMPLEX_TIMEOUT } else { 300 }
$ENTERPRISE_TIMEOUT = if ($env:ENTERPRISE_TIMEOUT) { [int]$env:ENTERPRISE_TIMEOUT } else { 600 }

# Minimum success rate
$MIN_SUCCESS_RATE = if ($env:MIN_SUCCESS_RATE) { [int]$env:MIN_SUCCESS_RATE } else { 50 }

# Test selection
$TEST_SELECTION = if ($env:TEST_SELECTION) { $env:TEST_SELECTION } else { "all" }
$TEST_COUNT = if ($env:TEST_COUNT) { [int]$env:TEST_COUNT } else { 3 }
$EXECUTION_ORDER = if ($env:EXECUTION_ORDER) { $env:EXECUTION_ORDER } else { "random" }
$VERBOSE = $false
if ($env:VERBOSE -and $env:VERBOSE -eq "true") { $VERBOSE = $true }
$SKIP_SERVER_CHECK = $false
if ($env:SKIP_SERVER_CHECK -and $env:SKIP_SERVER_CHECK -eq "true") { $SKIP_SERVER_CHECK = $true }

# Colors
$RED = "`e[0;31m"
$GREEN = "`e[0;32m"
$YELLOW = "`e[1;33m"
$BLUE = "`e[0;34m"
$PURPLE = "`e[0;35m"
$CYAN = "`e[0;36m"
$BOLD = "`e[1m"
$NC = "`e[0m"

# Counters
$script:TOTAL_TESTS = 0
$script:EXECUTED_TESTS = 0
$script:PASSED_TESTS = 0
$script:FAILED_TESTS = 0
$script:SKIPPED_TESTS = 0
$script:TIMED_OUT_TESTS = 0

function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[{0}] {1}" -f $timestamp, $Message
    $line | Tee-Object -FilePath $LOG_FILE -Append | Out-Null
}

function Write-VLog {
    param([string]$Message)
    if ($VERBOSE) {
        Write-Log ("{0}[VERBOSE]{1} {2}" -f $CYAN, $NC, $Message)
    }
}

function Ensure-Directories {
    New-Item -ItemType Directory -Force -Path $TEST_RESULTS_DIR | Out-Null
}

function Shuffle-Array {
    param([object[]]$Array)
    $arr = $Array.Clone()
    for ($i = $arr.Length - 1; $i -gt 0; $i--) {
        $j = Get-Random -Minimum 0 -Maximum ($i + 1)
        $tmp = $arr[$i]
        $arr[$i] = $arr[$j]
        $arr[$j] = $tmp
    }
    return ,$arr
}

function Test-Server {
    Write-Log ("{0}[INFO]{1} Checking Browser4 server at {2}..." -f $BLUE, $NC, $API_BASE)
    & curl.exe -s --connect-timeout 5 --max-time 10 "$API_BASE/actuator/health" > $null 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) {
        Write-Log ("{0}[SUCCESS]{1} Browser4 server is healthy and responding" -f $GREEN, $NC)
        return $true
    }
    & curl.exe -s --connect-timeout 5 --max-time 10 "$API_BASE/" > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Log ("{0}[WARNING]{1} Server responding but health check endpoint unavailable" -f $YELLOW, $NC)
        return $true
    }
    Write-Log ("{0}[ERROR]{1} Browser4 server not accessible at {2}" -f $RED, $NC, $API_BASE)
    Write-Log ("{0}[HINT]{1} Start Browser4 with:" -f $CYAN, $NC)
    Write-Log ("    {0}java -DOPENROUTER_API_KEY=${{OPENROUTER_API_KEY}} -jar Browser4.jar{1}" -f $BOLD, $NC)
    return $false
}

function Get-Description {
    param([string]$File)
    Get-Content -Path $File |
        Where-Object { $_ -match '^#' } |
        ForEach-Object { $_ -replace '^#\s*', '' } |
        Select-Object -First 5
}

function Get-Level {
    param([string]$File)
    $line = Get-Content -Path $File | Where-Object { $_ -match '^#\s*Level:' } | Select-Object -First 1
    if ($line) {
        return ($line -replace '^#\s*Level:\s*', '')
    }
    return "Simple"
}

function Get-TaskContent {
    param([string]$File)
    Get-Content -Path $File | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '\S' }
}

function Get-TimeoutForLevel {
    param([string]$Level)
    if ($Level -match "Simple") { return $SIMPLE_TIMEOUT }
    if ($Level -match "Complex") { return $COMPLEX_TIMEOUT }
    if ($Level -match "Enterprise") { return $ENTERPRISE_TIMEOUT }
    return $DEFAULT_TIMEOUT
}

function Test-HttpSuccess {
    param([string]$StatusCode)
    return ($StatusCode -match '^[23][0-9][0-9]$')
}

function Run-UseCaseTest {
    param(
        [string]$UseCaseFile,
        [int]$TestNumber,
        [int]$TotalTests
    )

    $filename = Split-Path -Path $UseCaseFile -Leaf
    $testName = $filename -replace '\.txt$', ''

    $script:TOTAL_TESTS++

    Write-Log ""
    Write-Log "===================================================================="
    Write-Log ("{0}[TEST {1}/{2}]{3} {4}{5}" -f $PURPLE, $TestNumber, $TotalTests, $NC, $BOLD, $testName)
    Write-Log "--------------------------------------------------------------------"

    $description = Get-Description -File $UseCaseFile
    $level = Get-Level -File $UseCaseFile
    $taskContentLines = Get-TaskContent -File $UseCaseFile
    $taskContent = $taskContentLines -join "`n"
    $timeout = Get-TimeoutForLevel -Level $level

    Write-Log ("{0}[DESCRIPTION]{1}" -f $CYAN, $NC)
    $description | ForEach-Object { Write-Log ("    {0}" -f $_) }
    Write-Log ("{0}[LEVEL]{1} {2}" -f $CYAN, $NC, $level)
    Write-Log ("{0}[TIMEOUT]{1} {2}s" -f $CYAN, $NC, $timeout)

    if ($VERBOSE) {
        Write-Log ("{0}[TASK CONTENT]{1}" -f $CYAN, $NC)
        $taskContentLines | ForEach-Object { Write-Log ("    {0}" -f $_) }
    }

    $statusFile = Join-Path $TEST_RESULTS_DIR "${testName}_status.log"
    $resultFile = Join-Path $TEST_RESULTS_DIR "${testName}_result.json"
    "" | Out-File -FilePath $statusFile -Encoding UTF8

    $startTime = Get-Date
    $startEpoch = [int][double]::Parse((Get-Date -Date $startTime -UFormat %s))
    Write-Log ("{0}[INFO]{1} Start time: {2}" -f $BLUE, $NC, $startTime.ToString("yyyy-MM-dd HH:mm:ss zzz"))

    Write-Log ("{0}[INFO]{1} Sending task to server using async plain command..." -f $BLUE, $NC)

    $submitOutput = & curl.exe -s -w "|%{http_code}" --connect-timeout 5 --max-time 15 -X POST -H "Content-Type: text/plain" --data $taskContent "$COMMAND_ENDPOINT"
    $submitExit = $LASTEXITCODE

    $submitHttp = $submitOutput.Substring($submitOutput.LastIndexOf("|") + 1)
    $commandId = $submitOutput.Substring(0, $submitOutput.LastIndexOf("|"))
    $commandId = $commandId.Trim("`n", "`r", '"')

    $testPassed = $false
    $testResult = ""

    if ($submitExit -ne 0) {
        $script:FAILED_TESTS++
        $script:EXECUTED_TESTS++
        Write-Log ("{0}[FAIL]{1} ❌ Failed to submit command (exit code: {2})" -f $RED, $NC, $submitExit)
        Write-Log ("{0}[INFO]{1} End time: {2} (Duration: {3}s)" -f $BLUE, $NC, (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"), ([int](Get-Date -UFormat %s) - $startEpoch))
        Write-Log "===================================================================="
        return $false
    }

    if (-not $commandId -or -not (Test-HttpSuccess -StatusCode $submitHttp)) {
        $script:FAILED_TESTS++
        $script:EXECUTED_TESTS++
        Write-Log ("{0}[FAIL]{1} ❌ Invalid response from server (HTTP: {2}, id: '{3}')" -f $RED, $NC, $submitHttp, $commandId)
        Write-Log ("{0}[INFO]{1} End time: {2} (Duration: {3}s)" -f $BLUE, $NC, (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"), ([int](Get-Date -UFormat %s) - $startEpoch))
        Write-Log "===================================================================="
        return $false
    }

    Write-Log ("{0}[INFO]{1} Command ID: {2}" -f $BLUE, $NC, $commandId)

    $statusUrl = "$COMMAND_STATUS_BASE/$commandId/status"
    $resultUrl = "$COMMAND_STATUS_BASE/$commandId/result"
    $lastStatusContent = ""
    $lastChangeEpoch = [int](Get-Date -UFormat %s)
    $staleIntervals = 0

    while ($true) {
        $statusOutput = & curl.exe -s -w "|%{http_code}" --max-time $timeout "$statusUrl"
        $statusExit = $LASTEXITCODE

        if ($statusExit -ne 0) {
            $script:FAILED_TESTS++
            $script:EXECUTED_TESTS++
            $testResult = "FAIL"
            Write-Log ("{0}[FAIL]{1} ❌ Failed to query status (exit code: {2})" -f $RED, $NC, $statusExit)
            break
        }

        $statusHttp = $statusOutput.Substring($statusOutput.LastIndexOf("|") + 1)
        $statusBody = $statusOutput.Substring(0, $statusOutput.LastIndexOf("|"))

        if (-not (Test-HttpSuccess -StatusCode $statusHttp)) {
            $script:FAILED_TESTS++
            $script:EXECUTED_TESTS++
            $testResult = "FAIL"
            Write-Log ("{0}[FAIL]{1} ❌ Status HTTP error: {2}" -f $RED, $NC, $statusHttp)
            break
        }

        if ($statusBody -ne $lastStatusContent) {
            "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $statusBody | Out-File -FilePath $statusFile -Append -Encoding UTF8
            $lastStatusContent = $statusBody
            $lastChangeEpoch = [int](Get-Date -UFormat %s)
            $staleIntervals = 0
        } else {
            $nowEpoch = [int](Get-Date -UFormat %s)
            if (($nowEpoch - $lastChangeEpoch) -ge 30) {
                $staleIntervals++
                $lastChangeEpoch = $nowEpoch
                Write-Log ("{0}[WAIT]{1} Status unchanged for {2}s (id: {3})" -f $YELLOW, $NC, $staleIntervals * 30, $commandId)
            }
        }

        if ($statusBody -match "isDone.*true" -or $statusBody -match "done.*true") {
            Write-Log ("{0}[INFO]{1} Task completed, fetching result..." -f $BLUE, $NC)
            $resultOutput = & curl.exe -s -w "|%{http_code}" --max-time $timeout "$resultUrl"
            $resultExit = $LASTEXITCODE

            if ($resultExit -ne 0) {
                $script:FAILED_TESTS++
                $script:EXECUTED_TESTS++
                $testResult = "FAIL"
                Write-Log ("{0}[FAIL]{1} ❌ Failed to fetch result (exit code: {2})" -f $RED, $NC, $resultExit)
            } else {
                $resultHttp = $resultOutput.Substring($resultOutput.LastIndexOf("|") + 1)
                $resultBody = $resultOutput.Substring(0, $resultOutput.LastIndexOf("|"))
                if (Test-HttpSuccess -StatusCode $resultHttp) {
                    $resultBody | Out-File -FilePath $resultFile -Encoding UTF8
                    $script:PASSED_TESTS++
                    $script:EXECUTED_TESTS++
                    $testResult = "PASS"
                    $testPassed = $true
                    Write-Log ("{0}[PASS]{1} ✅ Result saved to {2}" -f $GREEN, $NC, $resultFile)
                } else {
                    $script:FAILED_TESTS++
                    $script:EXECUTED_TESTS++
                    $testResult = "FAIL"
                    Write-Log ("{0}[FAIL]{1} ❌ Result HTTP error: {2}" -f $RED, $NC, $resultHttp)
                }
            }
            break
        }

        if ($staleIntervals -ge 3) {
            $script:TIMED_OUT_TESTS++
            $testResult = "TIMEOUT"
            Write-Log ("{0}[TIMEOUT]{1} Status not updated for 90 seconds, marking as timeout" -f $YELLOW, $NC)
            break
        }

        Start-Sleep -Seconds 1
    }

    $endTime = Get-Date
    $endEpoch = [int](Get-Date -UFormat %s)
    $duration = $endEpoch - $startEpoch

    Write-Log ("{0}[INFO]{1} End time: {2} (Duration: {3}s)" -f $BLUE, $NC, $endTime.ToString("yyyy-MM-dd HH:mm:ss zzz"), $duration)
    Write-Log "===================================================================="

    return $testPassed
}

function Run-AllTests {
    Write-Log ("{0}[INFO]{1} {2}Starting Use Case Test Suite{3}" -f $BLUE, $NC, $BOLD, $NC)
    Write-Log ("{0}[INFO]{1} API Endpoint: {2}" -f $BLUE, $NC, $COMMAND_ENDPOINT)
    Write-Log ("{0}[INFO]{1} Use Cases Directory: {2}" -f $BLUE, $NC, $USE_CASES_DIR)

    $useCaseFiles = Get-ChildItem -Path $USE_CASES_DIR -Filter "*.txt" -File -Recurse | Sort-Object FullName | ForEach-Object { $_.FullName }
    $totalFiles = $useCaseFiles.Count

    if ($totalFiles -eq 0) {
        Write-Log ("{0}[ERROR]{1} No use case files found in {2}" -f $RED, $NC, $USE_CASES_DIR)
        exit 1
    }

    Write-Log ("{0}[INFO]{1} Found {2} use case files" -f $BLUE, $NC, $totalFiles)

    $testsToRun = @()
    if ($TEST_SELECTION -eq "all") {
        $testsToRun = $useCaseFiles
    } else {
        $selectedNumbers = $TEST_SELECTION -split ","
        foreach ($num in $selectedNumbers) {
            $trimmed = $num.Trim()
            foreach ($file in $useCaseFiles) {
                if ((Split-Path -Path $file -Leaf).StartsWith($trimmed, [System.StringComparison]::OrdinalIgnoreCase)) {
                    $testsToRun += $file
                    break
                }
            }
        }
    }

    $totalSelected = $testsToRun.Count
    Write-Log ("{0}[INFO]{1} Tests selected: {2} | Order: {3} | Count: {4}" -f $BLUE, $NC, $totalSelected, $EXECUTION_ORDER, $TEST_COUNT)

    if (-not ($TEST_COUNT -is [int]) -or $TEST_COUNT -le 0) {
        Write-Log ("{0}[ERROR]{1} TEST_COUNT must be a positive integer (got: {2})" -f $RED, $NC, $TEST_COUNT)
        exit 1
    }

    if ($EXECUTION_ORDER -eq "random") {
        $testsToRun = Shuffle-Array -Array $testsToRun
    }

    if ($testsToRun.Count -gt $TEST_COUNT) {
        $testsToRun = $testsToRun[0..($TEST_COUNT - 1)]
    }

    $totalSelected = $testsToRun.Count
    Write-Log ("{0}[INFO]{1} Tests to execute after limiting: {2}" -f $BLUE, $NC, $totalSelected)

    $testCounter = 0
    foreach ($useCaseFile in $testsToRun) {
        $testCounter++
        Run-UseCaseTest -UseCaseFile $useCaseFile -TestNumber $testCounter -TotalTests $totalSelected | Out-Null
    }
}

function Print-Summary {
    $successRate = 0
    if ($script:EXECUTED_TESTS -gt 0) {
        $successRate = [int]([math]::Floor($script:PASSED_TESTS * 100 / $script:EXECUTED_TESTS))
    }

    Write-Log ""
    Write-Log "===================================================================="
    Write-Log ("{0}[FINAL SUMMARY]{1} {2}Use Case Test Results{3}" -f $BLUE, $NC, $BOLD, $NC)
    Write-Log "===================================================================="
    Write-Log ("{0}Test Session:{1} {2}" -f $BLUE, $NC, (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))
    Write-Log ("{0}Server:{1} {2}" -f $BLUE, $NC, $API_BASE)
    Write-Log ("{0}Use Cases Directory:{1} {2}" -f $BLUE, $NC, $USE_CASES_DIR)
    Write-Log "--------------------------------------------------------------------"
    Write-Log ("{0}Total Tests:{1} {2}" -f $BLUE, $NC, $script:TOTAL_TESTS)
    Write-Log ("{0}Executed:{1} {2}" -f $BLUE, $NC, $script:EXECUTED_TESTS)
    Write-Log ("{0}Passed:{1} {2}" -f $GREEN, $NC, $script:PASSED_TESTS)
    Write-Log ("{0}Failed:{1} {2}" -f $RED, $NC, $script:FAILED_TESTS)
    Write-Log ("{0}Timed Out:{1} {2}" -f $YELLOW, $NC, $script:TIMED_OUT_TESTS)
    Write-Log ("{0}Skipped:{1} {2}" -f $YELLOW, $NC, $script:SKIPPED_TESTS)
    Write-Log ("{0}Success Rate:{1} {2}% (minimum required: {3}%)" -f $BLUE, $NC, $successRate, $MIN_SUCCESS_RATE)
    Write-Log ("{0}Log File:{1} {2}" -f $BLUE, $NC, $LOG_FILE)
    Write-Log ("{0}Results Directory:{1} {2}" -f $BLUE, $NC, $TEST_RESULTS_DIR)
    Write-Log "===================================================================="

    if ($script:EXECUTED_TESTS -eq 0) {
        Write-Log ("{0}[WARNING]{1} No tests were executed" -f $YELLOW, $NC)
        exit 1
    }

    if ($successRate -ge $MIN_SUCCESS_RATE) {
        Write-Log ("{0}[SUCCESS]{1} Overall result: PASS 🎉" -f $GREEN, $NC)
        exit 0
    } else {
        Write-Log ("{0}[FAILURE]{1} Overall result: FAIL (success rate below {2}%)" -f $RED, $NC, $MIN_SUCCESS_RATE)
        exit 1
    }
}

function Show-Usage {
    @"
Usage: run-e2e-tests-agents.ps1 [OPTIONS]

Run use case based end-to-end tests against Browser4 server.

OPTIONS:
    -u, --url URL           Browser4 base URL (default: http://localhost:8182)
    -t, --test SELECTION    Test selection (comma-separated numbers or "all", default: all)
                            Examples: "01,02,03" or "01-ecommerce" or "all"
    -n, --count N           Number of tests to run after selection (default: 3)
    -o, --order MODE        Execution order: random | sequential (default: random)
    -s, --skip-server       Skip server connectivity check
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

ENVIRONMENT VARIABLES:
    API_BASE                Browser4 base URL
    DEFAULT_TIMEOUT         Default timeout in seconds (default: 300)
    SIMPLE_TIMEOUT          Timeout for simple tests (default: 180)
    COMPLEX_TIMEOUT         Timeout for complex tests (default: 300)
    ENTERPRISE_TIMEOUT      Timeout for enterprise tests (default: 600)
    MIN_SUCCESS_RATE        Minimum success rate to pass (default: 50)
    TEST_SELECTION          Same as --test option
    TEST_COUNT              Same as --count option (default: 3)
    EXECUTION_ORDER         Same as --order option (default: random)
    VERBOSE                 Same as --verbose option
    SKIP_SERVER_CHECK       Same as --skip-server option

EXAMPLES:
    .\run-e2e-tests-agents.ps1                 # Run 3 random tests from all use cases
    .\run-e2e-tests-agents.ps1 -u http://localhost:8080
    .\run-e2e-tests-agents.ps1 -t "01,02,03"
    .\run-e2e-tests-agents.ps1 -n 5 -o sequential
    .\run-e2e-tests-agents.ps1 --skip-server -v
"@
}

function Parse-Args {
    param([string[]]$Arguments)
    $i = 0
    while ($i -lt $Arguments.Length) {
        switch ($Arguments[$i]) {
            "-u" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $API_BASE = $Arguments[$i + 1]; $i += 2; continue }
            "--url" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $API_BASE = $Arguments[$i + 1]; $i += 2; continue }
            "-t" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $TEST_SELECTION = $Arguments[$i + 1]; $i += 2; continue }
            "--test" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $TEST_SELECTION = $Arguments[$i + 1]; $i += 2; continue }
            "-n" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $TEST_COUNT = [int]$Arguments[$i + 1]; $i += 2; continue }
            "--count" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $TEST_COUNT = [int]$Arguments[$i + 1]; $i += 2; continue }
            "-o" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $EXECUTION_ORDER = $Arguments[$i + 1]; $i += 2; continue }
            "--order" { if ($i + 1 -ge $Arguments.Length) { Show-Usage; exit 1 }; $EXECUTION_ORDER = $Arguments[$i + 1]; $i += 2; continue }
            "-s" { $SKIP_SERVER_CHECK = $true; $i++; continue }
            "--skip-server" { $SKIP_SERVER_CHECK = $true; $i++; continue }
            "-v" { $VERBOSE = $true; $i++; continue }
            "--verbose" { $VERBOSE = $true; $i++; continue }
            "-h" { Show-Usage; exit 0 }
            "--help" { Show-Usage; exit 0 }
            default { Write-Host "Unknown option: $($Arguments[$i])"; Show-Usage; exit 1 }
        }
    }
}

function Register-CancelHandler {
    try {
        Register-EngineEvent -SourceIdentifier "E2EConsoleCancel" -InputObject $Host -EventName "CancelKeyPress" -Action {
            Write-Log "`n${YELLOW}[INFO]${NC} Tests interrupted by user"
            exit 130
        } | Out-Null
    } catch {
        # Best effort
    }
}

function Main {
    Ensure-Directories
    Register-CancelHandler

    Write-Log ("{0}[INFO]{1} {2}Browser4 Use Case Test Suite{3}" -f $BLUE, $NC, $BOLD, $NC)
    Write-Log ("{0}[INFO]{1} Timestamp: {2}" -f $BLUE, $NC, (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))
    Write-Log ("{0}[INFO]{1} Server URL: {2}" -f $BLUE, $NC, $API_BASE)
    Write-Log ("{0}[INFO]{1} Verbose Mode: {2}" -f $BLUE, $NC, $VERBOSE)

    if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
        Write-Log ("{0}[ERROR]{1} curl command not found. Please install curl." -f $RED, $NC)
        exit 1
    }

    if (-not $SKIP_SERVER_CHECK) {
        if (-not (Test-Server)) {
            Write-Log ("{0}[WARNING]{1} Use --skip-server to bypass server check" -f $YELLOW, $NC)
            exit 1
        }
    }

    if (-not (Test-Path $USE_CASES_DIR)) {
        Write-Log ("{0}[ERROR]{1} Use cases directory not found: {2}" -f $RED, $NC, $USE_CASES_DIR)
        exit 1
    }

    Run-AllTests
    Print-Summary
}

Parse-Args -Arguments $args
Update-Endpoints
Main
